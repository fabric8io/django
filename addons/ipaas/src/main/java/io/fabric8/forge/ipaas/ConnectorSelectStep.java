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

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.forge.ipaas.repository.ConnectionRepository;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

import static io.fabric8.forge.addon.utils.CamelProjectHelper.hasDependency;

@FacetConstraint({ResourcesFacet.class})
public class ConnectorSelectStep extends AbstractIPaaSProjectCommand implements UIWizardStep {

    @Inject
    @WithAttributes(label = "Connector", required = true, description = "The connector to add")
    private UISelectOne<ConnectionCatalogDto> connectors;

    @Inject
    ConnectionRepository repository;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectorSelectStep.class)
                .name("iPaaS: Select Connector").category(Categories.create(CATEGORY))
                .description("Select Connector to add");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        String filter = (String) builder.getUIContext().getAttributeMap().get("filter");

        connectors.setValueChoices(repository.search(filter));
        connectors.setItemLabelConverter(ConnectionCatalogDto::getName);
        connectors.setValueConverter(s -> {
            for (ConnectionCatalogDto dto : connectors.getValueChoices()) {
                if (dto.getName().equals(s)) {
                    return dto;
                }
            }
            return null;
        });

        builder.add(connectors);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        ConnectionCatalogDto dto = connectors.getValue();

        Project project = getSelectedProject(context);

        // install connector as dependency
        if (!hasDependency(project, dto.getGroupId(), dto.getArtifactId(), dto.getVersion())) {
            DependencyBuilder component = DependencyBuilder.create().setGroupId(dto.getGroupId())
                    .setArtifactId(dto.getArtifactId()).setVersion(dto.getVersion());
            dependencyInstaller.install(project, component);
        }

        return Results.success("Added connector " + dto.getName());
    }

}
