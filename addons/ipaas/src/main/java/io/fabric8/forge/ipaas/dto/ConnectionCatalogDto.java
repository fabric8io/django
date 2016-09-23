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

    private String scheme;
    private String groupId;
    private String artifactId;
    private String version;
    private String javaType;

    private String name;
    private String description;
    private String type;
    private String[] labels;
    private String source;
    private Map<String, String> componentValues;
    private String[] endpointOptions;
    private Map<String, String> endpointValues;

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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
