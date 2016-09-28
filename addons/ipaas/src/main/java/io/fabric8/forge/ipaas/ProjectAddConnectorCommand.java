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

import javax.inject.Inject;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

@FacetConstraint({ResourcesFacet.class})
public class ProjectAddConnectorCommand extends AbstractIPaaSProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "Filter", required = false, description = "To filter connectors")
    private UIInput<String> filter;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ProjectAddConnectorCommand.class)
                .name("iPaaS: Add Connector").category(Categories.create(CATEGORY))
                .description("Adds a Connector to the project");
    }

    @Override
    protected boolean requiresCamelSetup() {
        return false;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(filter);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return null;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("filter", filter.getValue());

        return Results.navigateTo(ConnectorSelectStep.class);
    }
}
