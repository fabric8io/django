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

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import io.fabric8.forge.ipaas.dto.ComponentDto;
import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.forge.ipaas.helper.VersionHelper;
import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import static io.fabric8.forge.addon.utils.CamelProjectHelper.hasDependency;
import static io.fabric8.forge.addon.utils.OutputFormatHelper.toJson;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.createComponentDto;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.isComponentConsumerOnly;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.isComponentProducerOnly;
import static io.fabric8.forge.ipaas.helper.CamelCommandsHelper.asJavaClassName;
import static io.fabric8.forge.ipaas.helper.CamelCommandsHelper.asSchemeName;

@FacetConstraint({ResourcesFacet.class})
public class ConnectorSelectComponentStep extends AbstractIPaaSProjectCommand {

    @Inject
    @WithAttributes(label = "Camel Component", required = true, description = "The Camel component to use as connector")
    private UISelectOne<String> componentName;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private CamelCatalog camelCatalog;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(componentName);

        // should we limit the components to be either consumer or producer only
        boolean consumerOnly = false;
        boolean producerOnly = false;
        String source = (String) builder.getUIContext().getAttributeMap().get("source");
        if ("From".equals(source)) {
            consumerOnly = true;
        } else if ("To".equals(source)) {
            producerOnly = true;
        }

        List<String> filtered = new ArrayList<>();
        if (consumerOnly) {
            for (String name : camelCatalog.findComponentNames()) {
                // skip if the component is ONLY for producer (yes its correct)
                if (isComponentProducerOnly(camelCatalog, name)) {
                    continue;
                }
                filtered.add(name);
            }
        } else if (producerOnly) {
            for (String name : camelCatalog.findComponentNames()) {
                // skip if the component is ONLY for consumer (yes its correct)
                if (isComponentConsumerOnly(camelCatalog, name)) {
                    continue;
                }
                filtered.add(name);
            }
        } else {
            // include them all
            filtered.addAll(camelCatalog.findComponentNames());
        }

        componentName.setValueChoices(filtered);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String name = (String) context.getUIContext().getAttributeMap().get("name");
        String description = (String) context.getUIContext().getAttributeMap().get("description");
        String type = (String) context.getUIContext().getAttributeMap().get("type");
        String labels = (String) context.getUIContext().getAttributeMap().get("labels");
        String source = (String) context.getUIContext().getAttributeMap().get("source");

        Project project = getSelectedProject(context);

        FileResource<?> fileResource = getCamelConnectorFile(context);
        if (fileResource != null && fileResource.exists()) {
            // avoid overriding existing file
            return Results.fail("Connector file src/main/resources/camel-connector.json already exists.");
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
        String scheme = componentName.getValue();
        ComponentDto dto = createComponentDto(camelCatalog, scheme);
        if (dto == null) {
            return Results.fail("Cannot find camel component with name " + scheme);
        }

        // install component as dependency
        if (!hasDependency(project, dto.getGroupId(), dto.getArtifactId(), core.getCoordinate().getVersion())) {
            DependencyBuilder component = DependencyBuilder.create().setGroupId(dto.getGroupId())
                    .setArtifactId(dto.getArtifactId()).setVersion(core.getCoordinate().getVersion());
            dependencyInstaller.install(project, component);
        }

        // install camel-connector as dependency
        String version = new VersionHelper().getVersion();
        if (!hasDependency(project, "io.fabric8.django", "camel-connector", version)) {
            DependencyBuilder component = DependencyBuilder.create().setGroupId("io.fabric8.django")
                    .setArtifactId("camel-connector").setVersion(version);
            dependencyInstaller.install(project, component);
        }

        ConnectionCatalogDto catalog = new ConnectionCatalogDto();
        catalog.setScheme(scheme);
        catalog.setGroupId(dto.getGroupId());
        catalog.setArtifactId(dto.getArtifactId());
        catalog.setVersion(dto.getVersion());
        catalog.setName(name);
        catalog.setDescription(description);
        catalog.setType(type);
        if (labels != null) {
            catalog.setLabels(labels.split(","));
        }
        catalog.setSource(source);

        // add connector-maven-plugin if missing
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8.django", "connector-maven-plugin", version));
        plugin.addExecution(ExecutionBuilder.create().setId("connector").addGoal("jar"));
        if (!pluginFacet.hasPlugin(plugin.getCoordinate())) {
            pluginFacet.addPlugin(plugin);
        }

        // marshal DTO
        String json = toJson(catalog);

        if (fileResource == null) {
            fileResource = getCamelConnectorFile(context);
        }

        // write the connector json file
        fileResource.createNewFile();
        fileResource.setContents(json);

        // create Camel component file
        String className = asJavaClassName(name) + "Component";
        String schemeName = asSchemeName(name);
        String packageName = getBasePackageName(project);
        FileResource<?> comp = getCamelComponentFile(project, schemeName);
        comp.createNewFile();
        comp.setContents("class=" + packageName + "." + className);

        // create Java source code for component

        return Results.success("Created connector " + name);
    }

    protected FileResource getCamelComponentFile(Project project, String scheme) {
        if (project != null && project.hasFacet(ResourcesFacet.class)) {
            ResourcesFacet facet = project.getFacet(ResourcesFacet.class);
            return facet.getResource("META-INF/services/org/apache/camel/component/" + scheme);
        } else {
            return null;
        }
    }

    protected String getBasePackageName(Project project) {
        if (project != null && project.hasFacet(JavaSourceFacet.class)) {
            JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
            String base = java.getBasePackage();
            return base;
        }
        return null;
    }

}
