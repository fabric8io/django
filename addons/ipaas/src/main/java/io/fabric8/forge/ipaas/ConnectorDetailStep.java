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

import io.fabric8.forge.addon.utils.MavenHelpers;
import io.fabric8.forge.ipaas.dto.ComponentDto;
import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
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

        // add maven plugin stuff
        /*
          <build>
    <plugins>
      <plugin>
        <groupId>io.fabric8.django</groupId>
        <artifactId>connector-maven-plugin</artifactId>
        <version>2.3-SNAPSHOT</version>
        <executions>
          <execution>
            <goals>
              <goal>jar</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
         */


        // add connector-maven-plugin
        String version = MavenHelpers.getVersion("io.fabric8.django", "ipaas");
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8.django", "connector-maven-plugin", version));
        plugin.addExecution(ExecutionBuilder.create().setId("connector").addGoal("jar"));
        pluginFacet.addPlugin(plugin);

        // marshal DTO
        String json = toJson(catalog);

        // write the connector json file
        fileResource.createNewFile();
        fileResource.setContents(json);

        return Results.success("Created connector " + name.getValue());
    }

}
