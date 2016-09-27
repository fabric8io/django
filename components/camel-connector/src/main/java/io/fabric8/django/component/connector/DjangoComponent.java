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
package io.fabric8.django.component.connector;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;

public abstract class DjangoComponent extends DefaultComponent {

    private final CamelCatalog catalog = new DefaultCamelCatalog(false);

    private final String componentName;
    private final String className;

    public DjangoComponent(String componentName, String className) {
        this.componentName = componentName;
        this.className = className;

        // add to catalog
        catalog.addComponent(componentName, className);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        String scheme = null;
        Map<String, String> defaultOptions = new LinkedHashMap<>();

        Enumeration<URL> urls = getClass().getClassLoader().getResources("camel-connector.json");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            InputStream is = url.openStream();
            if (is != null) {
                List<String> lines = loadFile(is);
                IOHelper.close(is);

                String javaType = extractJavaType(lines);
                if (className.equals(javaType)) {
                    scheme = extractScheme(lines);

                    // extract the default options
                    boolean found = false;
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("\"endpointValues\":")) {
                            found = true;
                        } else if (line.startsWith("}")) {
                            found = false;
                        } else if (found) {
                            // "showAll":"true",
                            int pos = line.indexOf(':');
                            String key = line.substring(0, pos);
                            String value = line.substring(pos + 1);
                            if (value.endsWith(",")) {
                                value = value.substring(0, value.length() - 1);
                            }
                            key = StringHelper.removeLeadingAndEndingQuotes(key);
                            value = StringHelper.removeLeadingAndEndingQuotes(value);
                            defaultOptions.put(key, value);
                        }
                    }
                }
            }
        }

        if (scheme == null) {
            throw new IllegalArgumentException("Cannot find camel-connector.json in classpath for connector with uri: " + uri);
        }

        // gather all options to use when building the delegate uri
        Map<String, String> options = new LinkedHashMap<>();

        // default options from connector json
        if (!defaultOptions.isEmpty()) {
            options.putAll(defaultOptions);
        }
        // options from query parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = null;
            if (entry.getValue() != null) {
                value = entry.getValue().toString();
            }
            options.put(key, value);
        }
        parameters.clear();

        // add extra options from remaining (context-path)
        if (remaining != null) {
            String targetUri = scheme + ":" + remaining;
            Map<String, String> extra = catalog.endpointProperties(targetUri);
            if (extra != null && !extra.isEmpty()) {
                options.putAll(extra);
            }
        }

        String delegateUri = catalog.asEndpointUri(scheme, options, false);
        Endpoint delegate = getCamelContext().getEndpoint(delegateUri);

        return new DjangoEndpoint(uri, this, delegate);
    }

    private List<String> loadFile(InputStream fis) throws Exception {
        List<String> lines = new ArrayList<>();
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(fis));

        String line;
        do {
            line = reader.readLine();
            if (line != null) {
                lines.add(line);
            }
        } while (line != null);
        reader.close();

        return lines;
    }

    private String extractJavaType(List<String> json) {
        for (String line : json) {
            line = line.trim();
            if (line.startsWith("\"javaType\":")) {
                String answer = line.substring(12);
                return answer.substring(0, answer.length() - 2);
            }
        }
        return null;
    }

    private String extractScheme(List<String> json) {
        for (String line : json) {
            line = line.trim();
            if (line.startsWith("\"scheme\":")) {
                String answer = line.substring(10);
                return answer.substring(0, answer.length() - 2);
            }
        }
        return null;
    }

}

