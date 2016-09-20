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
package io.fabric8.forge.ipaas.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConnectionCatalogDto {

    private String id;

    private String scheme;
    private String name;
    private String type;
    private String[] labels;
    private Map<String, String> componentValues;
    private String[] endpointOptions;
    private Map<String, String> endpointValues;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getLabels() {
        return labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public String[] getEndpointOptions() {
        return endpointOptions;
    }

    public void setEndpointOptions(String[] endpointOptions) {
        this.endpointOptions = endpointOptions;
    }

    public Map<String, String> getEndpointValues() {
        return endpointValues;
    }

    public void setEndpointValues(Map<String, String> endpointValues) {
        this.endpointValues = endpointValues;
    }

    public Map<String, String> getComponentValues() {
        return componentValues;
    }

    public void setComponentValues(Map<String, String> componentValues) {
        this.componentValues = componentValues;
    }
}
