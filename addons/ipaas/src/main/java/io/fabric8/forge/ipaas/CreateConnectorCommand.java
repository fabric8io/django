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

import java.util.Arrays;
import javax.inject.Inject;

import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

@FacetConstraint({ResourcesFacet.class})
public class CreateConnectorCommand extends AbstractIPaaSProjectCommand implements UIWizard {

    private static String[] sources = new String[]{"Anywhere", "From", "To"};

    // TODO: name should be like Camel component name (eg lowercase and no digits/space, only dash)

    @Inject
    @WithAttributes(label = "Name", required = true, description = "Name of connector (only a..z and - chars permitted)")
    private UIInput<String> name;

    @Inject
    @WithAttributes(label = "Description", description = "Description of connector")
    private UIInput<String> description;

    @Inject
    @WithAttributes(label = "Type", description = "Type of connector")
    private UIInput<String> type;

    @Inject
    @WithAttributes(label = "Labels", description = "Labels of connector (separate by comma)")
    private UIInput<String> labels;

    @Inject
    @WithAttributes(label = "source", required = true, defaultValue = "Anywhere", description = "Where the connector can be used")
    private UISelectOne<String> source;

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
        builder.add(name).add(description).add(type).add(labels).add(source);
        source.setValueChoices(Arrays.asList(sources));
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return null;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("name", name.getValue());
        context.getUIContext().getAttributeMap().put("description", description.getValue());
        context.getUIContext().getAttributeMap().put("type", type.getValue());
        context.getUIContext().getAttributeMap().put("labels", labels.getValue());
        context.getUIContext().getAttributeMap().put("source", source.getValue());

        return Results.navigateTo(ConnectorSelectComponentStep.class);
    }
}
