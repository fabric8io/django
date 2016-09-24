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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.util.IOHelper;

public abstract class DjangoComponent extends DefaultComponent {

    private final CamelCatalog catalog = new DefaultCamelCatalog(false);

    public DjangoComponent(String componentName, String className) {
        catalog.addComponent(componentName, className);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        InputStream is = getCamelContext().getClassResolver().loadResourceAsStream("camel-connector.json");
        if (is == null) {
            throw new IllegalArgumentException("Cannot load camel-connector.json in the classpath");
        }

        List<String> lines = loadFile(is);
        IOHelper.close(is);

        String scheme = extractScheme(lines);

        Map<String, String> options = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = null;
            if (entry.getValue() != null) {
                value = entry.getValue().toString();
            }
            options.put(key, value);
        }
        parameters.clear();

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

