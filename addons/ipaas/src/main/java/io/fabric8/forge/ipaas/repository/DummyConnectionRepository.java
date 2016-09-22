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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;

/**
 * Used for testing purpose.
 */
@Deprecated
public class DummyConnectionRepository implements ConnectionRepository {

    private final Map<String, ConnectionCatalogDto> dtos = new LinkedHashMap<>();

    // some magic crappy forge/CDI will turn this into a manged bean
    // Managed Bean [class io.fabric8.forge.ipaas.repository.DummyConnectionRepository] with qualifiers [@Any @Default],
    // so we cannot use a @Produces

    public DummyConnectionRepository() {
    }

    @Override
    public List<ConnectionCatalogDto> search(String name, String type, String labels) {
        return null;
    }

    @Override
    public void editLabels(String id, String labels) {
        // noop
    }
}
