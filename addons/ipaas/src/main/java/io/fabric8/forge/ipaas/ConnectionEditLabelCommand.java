
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
package io.fabric8.forge.ipaas;

import javax.inject.Inject;

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

public class ConnectionEditLabelCommand extends AbstractIPaaSProjectCommand {

    @Inject
    @WithAttributes(label = "Id", required = true, description = "Connection Id")
    private UIInput<String> id;

    @Inject
    @WithAttributes(label = "Labels", description = "Labels to assign connector")
    private UIInput<String> labels;

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
        return Metadata.forCommand(ConnectionEditLabelCommand.class)
                .name("iPaaS: Edit Labels").category(Categories.create(CATEGORY))
                .description("Edit labels on existing connector");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(id).add(labels);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        repository.editLabels(id.getValue(), labels.getValue());
        return Results.success();
    }
}
