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
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.ipaas.dto.ComponentDto;
import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.forge.ipaas.helper.VersionHelper;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import static io.fabric8.forge.addon.utils.CamelProjectHelper.hasDependency;
import static io.fabric8.forge.addon.utils.OutputFormatHelper.toJson;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.createComponentDto;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.isComponentConsumerOnly;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.isComponentProducerOnly;
import static io.fabric8.forge.ipaas.helper.CamelCommandsHelper.asJavaClassName;
import static io.fabric8.forge.ipaas.helper.CamelCommandsHelper.asSchemeName;

@FacetConstraint({ResourcesFacet.class, JavaSourceFacet.class, MavenPluginFacet.class, MavenFacet.class})
public class ConnectorSelectComponentStep extends AbstractIPaaSProjectCommand implements UIWizardStep {

    @Inject
    @WithAttributes(label = "Camel Component", required = true, description = "The Camel component to use as connector")
    private UISelectOne<String> componentName;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private CamelCatalog camelCatalog;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectorSelectComponentStep.class)
                .name("iPaaS: Select Component").category(Categories.create(CATEGORY))
                .description("Select Camel Component to use as Connector");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {

        // should we add custom component first?
        String g = (String) builder.getUIContext().getAttributeMap().get("customGroupId");
        String a = (String) builder.getUIContext().getAttributeMap().get("customArtifactId");
        String v = (String) builder.getUIContext().getAttributeMap().get("customVersion");

        Set<String> customSchemes = null;
        if (g != null && a != null && v != null) {
            Project project = getSelectedProject(builder.getUIContext());
            customSchemes = addCustomComponent(project, g, a, v);
        }

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

        if (customSchemes != null && !customSchemes.isEmpty()) {
            String first = customSchemes.iterator().next();
            componentName.setDefaultValue(first);
        }
        componentName.getFacet(HintsFacet.class).setPromptInInteractiveMode(true);

        builder.add(componentName);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String name = (String) context.getUIContext().getAttributeMap().get("name");
        String description = (String) context.getUIContext().getAttributeMap().get("description");
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

        // favor using same version as camel-core (however it may be unknown) and if so use the version from the component dto
        String camelVersion = (core != null && core.getCoordinate() != null && core.getCoordinate().getVersion() != null) ? core.getCoordinate().getVersion() : dto.getVersion();

        // install Camel component as dependency
        if (!hasDependency(project, dto.getGroupId(), dto.getArtifactId(), camelVersion)) {
            DependencyBuilder component = DependencyBuilder.create().setGroupId(dto.getGroupId())
                    .setArtifactId(dto.getArtifactId()).setVersion(camelVersion);
            dependencyInstaller.install(project, component);
        }

        // install camel-connector as dependency
        String version = new VersionHelper().getVersion();
        if (!hasDependency(project, "io.fabric8.django", "camel-connector", version)) {
            DependencyBuilder component = DependencyBuilder.create().setGroupId("io.fabric8.django")
                    .setArtifactId("camel-connector").setVersion(version);
            dependencyInstaller.install(project, component);
        }

        // ensure scheme is valid
        String schemeName = asSchemeName(name);

        // create Camel component file
        String className = asJavaClassName(name) + "Component";
        String packageName = getBasePackageName(project); // use base package as target
        String javaType = packageName + "." + className;
        FileResource<?> comp = getCamelComponentFile(project, schemeName);
        if (comp != null) {
            comp.createNewFile();
            comp.setContents("class=" + javaType);
        }

        // create Java source code for component
        createJavaSourceForComponent(project, schemeName, packageName, className);

        ConnectionCatalogDto catalog = new ConnectionCatalogDto();

        // the base Camel component which the connector is using
        catalog.setBaseScheme(scheme);
        catalog.setBaseGroupId(dto.getGroupId());
        catalog.setBaseArtifactId(dto.getArtifactId());
        catalog.setBaseVersion(dto.getVersion());
        catalog.setBaseJavaType(dto.getJavaType());

        // the connector Maven GAV
        catalog.setJavaType(javaType);
        MavenFacet maven = project.getFacet(MavenFacet.class);
        catalog.setGroupId(maven.getModel().getGroupId());
        if (catalog.getGroupId() == null && maven.getModel().getParent() != null) {
            // maybe its inherited
            catalog.setGroupId(maven.getModel().getParent().getGroupId());
        }
        catalog.setArtifactId(maven.getModel().getArtifactId());
        catalog.setVersion("${project.version}");

        catalog.setScheme(schemeName);
        catalog.setName(name);
        catalog.setDescription(description);
        if (labels != null) {
            catalog.setLabels(labels.split(","));
        }
        catalog.setSource(source);

        // add maven filtering plugin if missing
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("org.apache.camel.maven.plugins", "maven-resources-plugin", "3.0.1"));
        if (!pluginFacet.hasPlugin(plugin.getCoordinate())) {
            pluginFacet.addPlugin(plugin);
        }

        // add camel maven plugin if missing
        plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("org.apache.camel", "camel-package-maven-plugin", camelVersion));
        plugin.addExecution(ExecutionBuilder.create().setId("prepare").addGoal("prepare-components").setPhase("generate-resources"));
        if (!pluginFacet.hasPlugin(plugin.getCoordinate())) {
            pluginFacet.addPlugin(plugin);
        }

        // add connector-maven-plugin if missing
        plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8.django", "connector-maven-plugin", version));
        plugin.addExecution(ExecutionBuilder.create().setId("connector").addGoal("jar"));
        if (!pluginFacet.hasPlugin(plugin.getCoordinate())) {
            pluginFacet.addPlugin(plugin);
        }

        // need to add <resource> on the model as forge do not have a builder for this
        Model model = maven.getModel();
        Resource resource = new Resource();
        resource.setDirectory("src/main/resources");
        resource.setFiltering(true);
        model.getBuild().getResources().add(resource);
        // set model to update it
        maven.setModel(model);

        // marshal DTO
        String json = toJson(catalog);

        if (fileResource == null) {
            fileResource = getCamelConnectorFile(context);
        }

        // write the connector json file
        fileResource.createNewFile();
        fileResource.setContents(json);

        return Results.success("Created connector " + name);
    }

    private void createJavaSourceForComponent(Project project, String schemeName, String packageName, String className) {
        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
        String fqn = packageName + "." + className;

        JavaResource existing = facet.getJavaResource(fqn);
        if (existing != null && existing.exists()) {
            // override existing
            existing.delete();
        }

        // need to parse to be able to extends another class
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setName(className);
        javaClass.setPackage(packageName);
        javaClass.setSuperType("DjangoComponent");
        javaClass.addImport("io.fabric8.django.component.connector.DjangoComponent");

        // add public no-arg constructor
        javaClass.addMethod().setPublic().setConstructor(true).setBody("super(\"" + schemeName + "\", \"" + fqn + "\");");

        facet.saveJavaSource(javaClass);
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

    private Set<String> addCustomComponent(Project project, String g, String a, String v) throws Exception {
        // download the JAR and look inside to find its javaType so we can add it to the catalog
        DependencyBuilder component = DependencyBuilder.create()
                .setGroupId(g).setArtifactId(a).setVersion(v);
        Dependency dependency = dependencyInstaller.install(project, component);

        if (dependency == null) {
            return null;
        }

        return discoverCustomCamelComponentsOnClasspathAndAddToCatalog(camelCatalog, project);
    }

}
