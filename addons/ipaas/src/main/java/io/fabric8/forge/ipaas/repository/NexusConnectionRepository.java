/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.forge.ipaas.repository;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.forge.ipaas.dto.NexusArtifactDto;
import io.fabric8.utils.IOHelpers;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.loadText;

@ApplicationScoped
public class NexusConnectionRepository implements ConnectionRepository {

    private final Set<NexusArtifactDto> indexedArtifacts = new LinkedHashSet<>();
    private final Map<NexusArtifactDto, ConnectionCatalogDto> connectors = new ConcurrentHashMap<>();
    private volatile ScheduledExecutorService executorService;
    private AtomicBoolean started = new AtomicBoolean();

    private static final String CLASSIFIER = "camel-connector";

    private Long delay = 60L; // use 60 second delay between index runs
    private String nexusUrl = "http://nexus/service/local/data_index";

    public String getNexusUrl() {
        return nexusUrl;
    }

    public void setNexusUrl(String nexusUrl) {
        this.nexusUrl = nexusUrl;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    // we want to eager start ourselves
    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        start();
    }

    @PostConstruct
    public void start() {
        if (started.compareAndSet(false, true)) {
            System.out.println("NexusConnectionRepository is already started");
            return;
        }

        System.out.println("Starting NexusConnectionRepository");

        if (nexusUrl == null || nexusUrl.isEmpty()) {
            System.out.println("Nexus service not found. Indexing Nexus is not enabled!");
            return;
        }

        System.out.println("Indexing Nexus every " + delay + " seconds interval");
        executorService = Executors.newScheduledThreadPool(1);

        executorService.scheduleWithFixedDelay(() -> {
            try {
                System.out.println("Indexing Nexus " + nexusUrl + " +++ start +++");
                indexNexus();
            } catch (Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                System.err.println("Error indexing Nexus " + nexusUrl + " due " + e.getMessage() + "\n" + sw.toString());
            } finally {
                System.out.println("Indexing Nexus " + nexusUrl + " +++ end +++");
            }
        }, 10, delay, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        started.set(false);
    }

    @Override
    public List<ConnectionCatalogDto> search(String filter, boolean latestVersionOnly) {
        List<ConnectionCatalogDto> answer = new ArrayList<>();

        if (filter == null || filter.isEmpty()) {
            // return all of them
            answer.addAll(connectors.values());
        } else {
            // search ignore case
            filter = filter.toLowerCase(Locale.US);
            for (ConnectionCatalogDto dto : connectors.values()) {
                if (dto.getName().toLowerCase(Locale.US).contains(filter)) {
                    answer.add(dto);
                } else if (dto.getDescription().toLowerCase(Locale.US).contains(filter)) {
                    answer.add(dto);
                } else if (dto.getGroupId().toLowerCase(Locale.US).contains(filter)) {
                    answer.add(dto);
                } else if (dto.getArtifactId().toLowerCase(Locale.US).contains(filter)) {
                    answer.add(dto);
                } else if (dto.getVersion().toLowerCase(Locale.US).contains(filter)) {
                    answer.add(dto);
                } else {
                    String[] labels = dto.getLabels();
                    if (labels != null && labels.length > 0) {
                        for (String lab : labels) {
                            lab = lab.toLowerCase(Locale.US);
                            if (lab.contains(filter)) {
                                answer.add(dto);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // filter only latest version
        if (latestVersionOnly && answer.size() > 1) {
            // sort first
            Collections.sort(answer, (o1, o2) -> o1.getMavenGav().compareTo(o2.getMavenGav()));

            // keep only latest in each group
            List<ConnectionCatalogDto> unique = new ArrayList<>();
            ConnectionCatalogDto prev = null;

            for (ConnectionCatalogDto dto : answer) {
                if (prev == null
                        || (prev.getGroupId().equals(dto.getGroupId()) && prev.getArtifactId().equals(dto.getArtifactId()))) {
                    prev = dto;
                } else {
                    unique.add(prev);
                    prev = dto;
                }
            }
            if (prev != null) {
                // special for last element
                unique.add(prev);
            }

            answer = unique;
        }

        return answer;
    }

    protected void indexNexus() throws Exception {
        // must have q parameter so use connector to find all connectors
        String query = nexusUrl + "?q=connector";
        URL url = new URL(query);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);

        DocumentBuilder documentBuilder = factory.newDocumentBuilder();

        InputStream is = url.openStream();
        Document dom = documentBuilder.parse(is);

        XPathFactory xpFactory = XPathFactory.newInstance();
        XPath exp = xpFactory.newXPath();
        NodeList list = (NodeList) exp.evaluate("//classifier[text() = '" + CLASSIFIER + "']", dom, XPathConstants.NODESET);

        Set<NexusArtifactDto> newArtifacts = new LinkedHashSet<>();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            Node parent = node.getParentNode();

            String g = getNodeText(parent.getChildNodes(), "groupId");
            String a = getNodeText(parent.getChildNodes(), "artifactId");
            String v = getNodeText(parent.getChildNodes(), "version");
            String l = getNodeText(parent.getChildNodes(), "artifactLink");

            if (g != null & a != null & v != null & l != null) {
                NexusArtifactDto dto = new NexusArtifactDto();
                dto.setGroupId(g);
                dto.setArtifactId(a);
                dto.setVersion(v);
                dto.setArtifactLink(l);

                System.out.println("Found connector: " + dto.getGroupId() + ":" + dto.getArtifactId() + ":" + dto.getVersion());

                // is it a new artifact
                boolean newArtifact = true;
                for (NexusArtifactDto existing : indexedArtifacts) {
                    if (existing.getGroupId().equals(dto.getGroupId())
                            && existing.getArtifactId().equals(dto.getArtifactId())
                            && existing.getVersion().equals(dto.getVersion())) {
                        newArtifact = false;
                        break;
                    }
                }
                if (newArtifact) {
                    newArtifacts.add(dto);
                }
            }
        }

        // now download the new artifact JARs and look inside to find more details
        for (NexusArtifactDto dto : newArtifacts) {
            try {
                // download using url classloader reader
                URL jarUrl = new URL(dto.getArtifactLink());
                String json = loadCamelConnectorJSonSchema(jarUrl);

                ObjectMapper mapper = new ObjectMapper();
                ConnectionCatalogDto cat = mapper.readerFor(ConnectionCatalogDto.class).readValue(json);

                indexedArtifacts.add(dto);
                connectors.putIfAbsent(dto, cat);
                System.out.println("Added connector: " + dto.getGroupId() + ":" + dto.getArtifactId() + ":" + dto.getVersion());
            } catch (Exception e) {
                System.err.println("Error downloading connector JAR " + dto.getArtifactLink() + ". This exception is ignored. " + e.getMessage());
            }
        }

        IOHelpers.close(is);
    }

    private static String loadCamelConnectorJSonSchema(URL url) {
        try {
            // is it a JAR file
            URLClassLoader child = new URLClassLoader(new URL[]{url});
            InputStream is = child.getResourceAsStream("camel-connector.json");
            if (is != null) {
                return loadText(is);
            }
            IOHelpers.close(is);
        } catch (Throwable e) {
            e.printStackTrace();
            // ignore
        }

        return null;
    }

    private static String getNodeText(NodeList list, String name) {
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (name.equals(child.getNodeName())) {
                return child.getTextContent();
            }
        }
        return null;
    }
}
