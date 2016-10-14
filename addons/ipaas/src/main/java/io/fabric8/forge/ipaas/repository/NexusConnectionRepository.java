/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.ipaas.repository;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.utils.IOHelpers;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.loadText;

public class NexusConnectionRepository implements ConnectionRepository {

    // TODO: schedule index as background task to run every X minutes
    // TODO: use kubernetes service lookup
    // TODO: newer version should replace older version?

    private static String NEXUS_URL = "http://nexus.fabric8.rh.fabric8.io/service/local/data_index";

    private final Set<NexusArtifactDto> indexedArtifacts = new LinkedHashSet<>();
    private final Map<NexusArtifactDto, ConnectionCatalogDto> connectors = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        NexusConnectionRepository me = new NexusConnectionRepository();
        me.indexNexus();

        // search for foo
        List<ConnectionCatalogDto> list = me.search("foo");
        System.out.println(list);

        list = me.search("cool");
        System.out.println(list);

        list = me.search("bar");
        System.out.println(list);
    }

    public void indexNexus() {
        try {
            String query = NEXUS_URL + "?q=connector";
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
            NodeList list = (NodeList) exp.evaluate("//classifier[text() = 'connector']", dom, XPathConstants.NODESET);

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

                    // is it a new artifact
                    if (!indexedArtifacts.contains(dto)) {
                        newArtifacts.add(dto);
                    }
                }
            }

            // now download the new artifact JARs and look inside to find more details
            for (NexusArtifactDto dto : newArtifacts) {
                // download using url classloader reader
                URL jarUrl = new URL(dto.getArtifactLink());
                String json = loadCamelConnectorJSonSchema(jarUrl);
                System.out.println(json);

                ObjectMapper mapper = new ObjectMapper();
                ConnectionCatalogDto cat = mapper.readerFor(ConnectionCatalogDto.class).readValue(json);
                connectors.putIfAbsent(dto, cat);
            }

            IOHelpers.close(is);

        } catch (Exception e) {
            System.out.println("Error indexing Nexus " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String loadCamelConnectorJSonSchema(URL url) {
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

    @Override
    public List<ConnectionCatalogDto> search(String filter) {
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

        return answer;
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
