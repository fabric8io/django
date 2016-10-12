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
package io.fabric8.forge.ipaas.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Map;

import io.fabric8.forge.ipaas.dto.ComponentDto;
import io.fabric8.utils.Strings;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;

public class CamelCatalogHelper {

    public static String endpointComponentName(String uri) {
        if (uri != null) {
            int idx = uri.indexOf(":");
            if (idx > 0) {
                return uri.substring(0, idx);
            }
        }
        return null;
    }

    /**
     * Checks whether the given key is a multi valued option
     *
     * @param scheme the component name
     * @param key    the option key
     * @return <tt>true</tt> if the key is multi valued, <tt>false</tt> otherwise
     */
    public static boolean isMultiValue(CamelCatalog camelCatalog, String scheme, String key) {
        // use the camel catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + scheme);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String multiValue = propertyMap.get("multiValue");
                if (key.equals(name)) {
                    return "true".equals(multiValue);
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given key is a multi valued option
     *
     * @param scheme the component name
     * @param key    the option key
     * @return <tt>true</tt> if the key is multi valued, <tt>false</tt> otherwise
     */
    public static String getPrefix(CamelCatalog camelCatalog, String scheme, String key) {
        // use the camel catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + scheme);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String prefix = propertyMap.get("prefix");
                if (key.equals(name)) {
                    return prefix;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the given value is matching the default value from the given component.
     *
     * @param scheme the component name
     * @param key    the option key
     * @param value  the option value
     * @return <tt>true</tt> if matching the default value, <tt>false</tt> otherwise
     */
    public static boolean isDefaultValue(CamelCatalog camelCatalog, String scheme, String key, String value) {
        // use the camel catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + scheme);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String defaultValue = propertyMap.get("defaultValue");
                if (key.equals(name)) {
                    return value.equalsIgnoreCase(defaultValue);
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given value corresponds to a dummy none placeholder for an enum type
     *
     * @param scheme the component name
     * @param key    the option key
     * @return <tt>true</tt> if matching the default value, <tt>false</tt> otherwise
     */
    public static boolean isNonePlaceholderEnumValue(CamelCatalog camelCatalog, String scheme, String key) {
        // use the camel catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + scheme);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String enums = propertyMap.get("enum");
                if (key.equals(name) && enums != null) {
                    if (!enums.contains("none")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether the component is consumer only
     */
    public static boolean isComponentConsumerOnly(CamelCatalog camelCatalog, String scheme) {
        // use the camel catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            return false;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String consumerOnly = propertyMap.get("consumerOnly");
                if (consumerOnly != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the component is consumer only
     */
    public static boolean isComponentProducerOnly(CamelCatalog camelCatalog, String scheme) {
        // use the camel catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            return false;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
                String consumerOnly = propertyMap.get("producerOnly");
                if (consumerOnly != null) {
                    return true;
                }
            }
        }
        return false;
    }


    public static ComponentDto createComponentDto(CamelCatalog camelCatalog, String scheme) {
        // use the camel catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            return null;
        }

        ComponentDto dto = new ComponentDto();
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        for (Map<String, String> row : data) {
            if (row.get("scheme") != null) {
                dto.setScheme(row.get("scheme"));
            } else if (row.get("syntax") != null) {
                dto.setSyntax(row.get("syntax"));
            } else if (row.get("title") != null) {
                dto.setTitle(row.get("title"));
            } else if (row.get("description") != null) {
                dto.setDescription(row.get("description"));
            } else if (row.get("label") != null) {
                String labelText = row.get("label");
                if (Strings.isNotBlank(labelText)) {
                    dto.setTags(labelText.split(","));
                }
            } else if (row.get("consumerOnly") != null) {
                dto.setConsumerOnly("true".equals(row.get("consumerOnly")));
            } else if (row.get("producerOnly") != null) {
                dto.setProducerOnly("true".equals(row.get("producerOnly")));
            } else if (row.get("javaType") != null) {
                dto.setJavaType(row.get("javaType"));
            } else if (row.get("groupId") != null) {
                dto.setGroupId(row.get("groupId"));
            } else if (row.get("artifactId") != null) {
                dto.setArtifactId(row.get("artifactId"));
            } else if (row.get("version") != null) {
                dto.setVersion(row.get("version"));
            }
        }
        return dto;
    }

    /**
     * Loads the entire stream into memory as a String and returns it.
     * <p/>
     * <b>Notice:</b> This implementation appends a <tt>\n</tt> as line
     * terminator at the of the text.
     * <p/>
     * Warning, don't use for crazy big streams :)
     */
    public static String loadText(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(in);
        try {
            BufferedReader reader = new LineNumberReader(isr);
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    builder.append(line);
                    builder.append("\n");
                } else {
                    break;
                }
            }
            return builder.toString();
        } finally {
            isr.close();
            in.close();
        }
    }

}
