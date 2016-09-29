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
package io.fabric8.forge.ipaas;

import javax.inject.Inject;

import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.projects.Project;
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
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

public class AddCustomComponentStep extends AbstractIPaaSProjectCommand implements UIWizardStep {

    @Inject
    @WithAttributes(label = "GroupId", required = true, description = "Maven GroupId")
    private UIInput<String> groupId;

    @Inject
    @WithAttributes(label = "ArtifactId", required = true, description = "Maven ArtifactId")
    private UIInput<String> artifactId;

    @Inject
    @WithAttributes(label = "Version", required = true, description = "Maven Version")
    private UIInput<String> version;

    @Inject
    protected CamelCatalog camelCatalog;

    @Override
    protected boolean isCamelProject(Project project) {
        return false;
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(AddCustomComponentStep.class)
                .name("iPaaS: Add Custom Camel Component").category(Categories.create(CATEGORY))
                .description("Adds a custom Camel component to the list of known components");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(groupId).add(artifactId).add(version);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("customGroupId", groupId.getValue());
        context.getUIContext().getAttributeMap().put("customArtifactId", artifactId.getValue());
        context.getUIContext().getAttributeMap().put("customVersion", version.getValue());

        return Results.navigateTo(ConnectorSelectComponentStep.class);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return null;
    }

}
