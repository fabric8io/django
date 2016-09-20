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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;

/**
 * Used for testing purpose.
 */
public class DummyConnectionRepository implements ConnectionRepository {

    private final Map<String, ConnectionCatalogDto> dtos = new LinkedHashMap<>();

    // some magic crappy forge/CDI will turn this into a manged bean
    // Managed Bean [class io.fabric8.forge.ipaas.repository.DummyConnectionRepository] with qualifiers [@Any @Default],
    // so we cannot use a @Produces

    public DummyConnectionRepository() {
        init();
    }

    @Override
    public List<ConnectionCatalogDto> search(String name, String type, String labels) {
        List<ConnectionCatalogDto> list = new ArrayList<>();

        // TODO: filter
        list.addAll(dtos.values());

        return list;
    }

    @Override
    public void editLabels(String id, String labels) {
        if (!labels.isEmpty()) {
            ConnectionCatalogDto dto = dtos.get(id);
            if (dto != null) {
                String[] parts = labels.split(",");
                dto.setLabels(parts);
            }
        }
    }

    private void init() {
        ConnectionCatalogDto dto = new ConnectionCatalogDto();
        dto.setId("123");

        dto.setScheme("twitter");
        dto.setType("business");
        dto.setName("My awesome connector");
        dto.setLabels(new String[]{"twitter"});
        dto.setEndpointOptions(new String[]{"keywords"});

        Map<String, String> values = new LinkedHashMap<>();
        values.put("type", "search");
        values.put("delay", "10000");
        dto.setEndpointValues(values);

        Map<String, String> values2 = new LinkedHashMap<>();
        values2.put("consumerKey", "ABC");
        values2.put("consumerSecret", "DEF");
        values2.put("accessToken", "GHI");
        values2.put("accessTokenSecret", "JKL");
        dto.setComponentValues(values2);

        dtos.put(dto.getId(), dto);
    }
}
