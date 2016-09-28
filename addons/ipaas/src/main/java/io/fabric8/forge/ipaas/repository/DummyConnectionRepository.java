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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;

/**
 * Used for testing purpose.
 */
@Deprecated
public class DummyConnectionRepository implements ConnectionRepository {

    private final Map<String, ConnectionCatalogDto> dtos = new LinkedHashMap<>();

    // some magic forge/CDI will turn this into a manged bean
    // Managed Bean [class io.fabric8.forge.ipaas.repository.DummyConnectionRepository] with qualifiers [@Any @Default],
    // so we cannot use a @Produces

    public DummyConnectionRepository() {
        addHardcodedValues();
    }

    @Override
    public List<ConnectionCatalogDto> search(String filter) {
        List<ConnectionCatalogDto> answer = new ArrayList<>();

        if (filter != null && !filter.isEmpty()) {
            filter = filter.toLowerCase(Locale.US);

            for (ConnectionCatalogDto dto : dtos.values()) {
                String name = dto.getName().toLowerCase(Locale.US);
                if (name.contains(filter)) {
                    answer.add(dto);
                } else if (dto.getLabels() != null) {
                    for (String label : dto.getLabels()) {
                        label = label.toLowerCase(Locale.US);
                        if (label.contains(filter)) {
                            answer.add(dto);
                            break;
                        }
                    }
                }
            }
        } else {
            answer.addAll(dtos.values());
        }

        return answer;
    }

    private void addHardcodedValues() {
        ConnectionCatalogDto foo = new ConnectionCatalogDto();
        foo.setName("Foo");
        foo.setLabels(new String[]{"foo", "timer"});
        foo.setGroupId("io.fabric8.django");
        foo.setArtifactId("foo-connector");
        foo.setVersion("2.3-SNAPSHOT");

        ConnectionCatalogDto bar = new ConnectionCatalogDto();
        bar.setName("Bar");
        bar.setLabels(new String[]{"bar", "beer"});
        bar.setGroupId("io.fabric8.django");
        bar.setArtifactId("bar-connector");
        bar.setVersion("2.3-SNAPSHOT");

        dtos.put("Foo", foo);
        dtos.put("Bar", bar);
    }

}
