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

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

public class CreateConnectorCommand extends AbstractIPaaSProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "Camel Component", required = true, description = "The Camel component to use as connector")
    private UISelectOne<String> componentName;

    @Inject
    protected CamelCatalog camelCatalog;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CreateConnectorCommand.class)
                .name("iPaaS: Create Connector").category(Categories.create(CATEGORY))
                .description("Create a new Connector");
    }

    @Override
    protected boolean requiresCamelSetup() {
        return false;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        List<String> names = new ArrayList<>();
        names.addAll(camelCatalog.findComponentNames());
        // names.add(0, "<none>");

        componentName.setValueChoices(names);
        builder.add(componentName);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return null;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        String scheme = componentName.getValue();
        context.getUIContext().getAttributeMap().put("scheme", scheme);

        return Results.navigateTo(ConnectorDetailStep.class);
    }
}
