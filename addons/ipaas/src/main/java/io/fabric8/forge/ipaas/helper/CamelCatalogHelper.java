/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.ipaas.helper;

import java.util.List;
import java.util.Map;

import io.fabric8.forge.ipaas.dto.ComponentDto;
import io.fabric8.utils.Strings;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;

public class CamelCatalogHelper {

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
}
