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

import io.fabric8.forge.ipaas.dto.ComponentDto;
import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import static io.fabric8.forge.addon.utils.OutputFormatHelper.toJson;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.createComponentDto;

@FacetConstraint({ResourcesFacet.class})
public class ConnectorDetailStep extends AbstractIPaaSProjectCommand {

    @Inject
    @WithAttributes(label = "Name", required = true, description = "Name of connector")
    private UIInput<String> name;

    @Inject
    @WithAttributes(label = "Type", description = "Type of connector")
    private UIInput<String> type;

    @Inject
    @WithAttributes(label = "Labels", description = "Labels of connector (separate by comma)")
    private UIInput<String> labels;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private CamelCatalog camelCatalog;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(name).add(type).add(labels);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        FileResource<?> fileResource = getCamelConnectorFile(context);
        if (fileResource.exists()) {
            // avoid overriding existing file
            return Results.fail("Connector file src/main/resources/camel-connector.json already exists.");
        }

        String scheme = (String) context.getUIContext().getAttributeMap().get("scheme");
        if (scheme == null) {
            return null;
        }

        // does the project already have camel?
        Dependency core = findCamelCoreDependency(project);
        if (core == null) {
            core = DependencyBuilder.create().setCoordinate(createCamelCoordinate("camel-core", null));
            // add camel-core
            dependencyInstaller.install(project, core);
            core = findCamelCoreDependency(project);
        }

        // find camel component based on scheme
        ComponentDto dto = createComponentDto(camelCatalog, scheme);
        if (dto == null) {
            return Results.fail("Cannot find camel component with name " + scheme);
        }

        DependencyBuilder component = DependencyBuilder.create().setGroupId(dto.getGroupId())
                .setArtifactId(dto.getArtifactId()).setVersion(core.getCoordinate().getVersion());
        dependencyInstaller.install(project, component);

        ConnectionCatalogDto catalog = new ConnectionCatalogDto();
        catalog.setScheme(scheme);
        catalog.setName(name.getValue());
        catalog.setType(type.getValue());
        if (labels.getValue() != null) {
            catalog.setLabels(labels.getValue().split(","));
        }

        // marshal DTO
        String json = toJson(catalog);

        // write the connector json file
        fileResource.createNewFile();
        fileResource.setContents(json);

        return Results.success("Created connector " + name.getValue());
    }

}
