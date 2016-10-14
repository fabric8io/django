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
package io.fabric8.forge.ipaas;

import java.util.List;
import javax.inject.Inject;

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.forge.ipaas.repository.ConnectionRepository;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import static io.fabric8.forge.addon.utils.OutputFormatHelper.toJson;

public class ConnectionSearchCommand extends AbstractIPaaSProjectCommand {

    @Inject
    @WithAttributes(label = "Filter", description = "Filter to use when searching")
    private UIInput<String> filter;

    @Inject
    private ConnectionRepository repository;

    public ConnectionRepository getRepository() {
        return repository;
    }

    public void setRepository(ConnectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isEnabled(UIContext context) {
        return true;
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectionSearchCommand.class)
                .name("iPaaS: Search Connectors").category(Categories.create(CATEGORY))
                .description("Search for connectors");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(filter);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        List<ConnectionCatalogDto> list = repository.search(filter.getValue());
        if (list.isEmpty()) {
            return Results.success();
        } else {
            String json = toJson(list);
            return Results.success(json);
        }
    }
}
