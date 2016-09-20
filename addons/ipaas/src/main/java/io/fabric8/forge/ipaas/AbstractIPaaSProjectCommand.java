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

import java.io.PrintStream;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIRegion;

public abstract class AbstractIPaaSProjectCommand extends AbstractProjectCommand {

    public static String CATEGORY = "ipaas";

    @Inject
    protected ProjectFactory projectFactory;

    @Inject
    protected ConverterFactory converterFactory;

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean enabled = super.isEnabled(context);
        if (!enabled) {
            return false;
        }
        if (requiresCamelSetup()) {
            // requires camel is already setup
            Project project = getSelectedProjectOrNull(context);
            if (project != null) {
                return findCamelCoreDependency(project) != null;
            }
        }
        return false;
    }

    protected Project getSelectedProjectOrNull(UIContext context) {
        return Projects.getSelectedProject(this.getProjectFactory(), context);
    }

    protected boolean isRunningInGui(UIContext context) {
        return context.getProvider().isGUI();
    }

    protected boolean requiresCamelSetup() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    protected ConverterFactory getConverterFactory() {
        return converterFactory;
    }

    protected PrintStream getOutput(UIExecutionContext context) {
        return context.getUIContext().getProvider().getOutput().out();
    }

    protected boolean isCamelProject(Project project) {
        // is there any camel dependency?
        return !findCamelArtifacts(project).isEmpty();
    }

    protected Dependency findCamelCoreDependency(Project project) {
        return CamelProjectHelper.findCamelCoreDependency(project);
    }

    protected Set<Dependency> findCamelArtifacts(Project project) {
        return CamelProjectHelper.findCamelArtifacts(project);
    }

    protected Coordinate createCoordinate(String groupId, String artifactId, String version) {
        CoordinateBuilder builder = CoordinateBuilder.create()
                .setGroupId(groupId)
                .setArtifactId(artifactId);
        if (version != null) {
            builder = builder.setVersion(version);
        }

        return builder;
    }

    protected Coordinate createCamelCoordinate(String artifactId, String version) {
        return createCoordinate("org.apache.camel", artifactId, version);
    }

    protected FileResource getXmlResourceFile(Project project, String xmlResourceName) {
        if (xmlResourceName == null) {
            return null;
        }

        ResourcesFacet facet = null;
        WebResourcesFacet webResourcesFacet = null;
        if (project.hasFacet(ResourcesFacet.class)) {
            facet = project.getFacet(ResourcesFacet.class);
        }
        if (project.hasFacet(WebResourcesFacet.class)) {
            webResourcesFacet = project.getFacet(WebResourcesFacet.class);
        }

        FileResource file = facet != null ? facet.getResource(xmlResourceName) : null;
        if (file == null || !file.exists()) {
            file = webResourcesFacet != null ? webResourcesFacet.getWebResource(xmlResourceName) : null;
        }
        return file;
    }

    protected String getSelectedFile(UIContext context) {
        String currentFile = null;
        // get selected file
        Optional<UIRegion<Object>> region = context.getSelection().getRegion();
        if (region.isPresent()) {
            Object resource = region.get().getResource();
            currentFile = resource.toString();
        }
        return currentFile;
    }

    protected boolean isSelectedFileJava(UIContext context) {
        Optional<UIRegion<Object>> region = context.getSelection().getRegion();
        if (region.isPresent()) {
            Object resource = region.get().getResource();
            if (resource instanceof FileResource) {
                return ((FileResource) resource).getFullyQualifiedName().endsWith(".java");
            }
        }
        return false;
    }

    protected boolean isSelectedFileXml(UIContext context) {
        Optional<UIRegion<Object>> region = context.getSelection().getRegion();
        if (region.isPresent()) {
            Object resource = region.get().getResource();
            if (resource instanceof FileResource) {
                return ((FileResource) resource).getFullyQualifiedName().endsWith(".xml");
            }
        }
        return false;
    }

    protected int getCurrentCursorLine(UIContext context) {
        int answer = -1;
        Optional<UIRegion<Object>> region = context.getSelection().getRegion();
        if (region.isPresent()) {
            answer = region.get().getStartLine();
        }
        return answer;
    }

    protected int getCurrentCursorPosition(UIContext context) {
        int answer = -1;
        Optional<UIRegion<Object>> region = context.getSelection().getRegion();
        if (region.isPresent()) {
            answer = region.get().getStartPosition();
        }
        return answer;
    }

    protected String asRelativeFile(UIContext context, String currentFile) {
        Project project = getSelectedProject(context);

        JavaSourceFacet javaSourceFacet = null;
        ResourcesFacet resourcesFacet = null;
        WebResourcesFacet webResourcesFacet = null;
        if (project.hasFacet(JavaSourceFacet.class)) {
            javaSourceFacet = project.getFacet(JavaSourceFacet.class);
        }
        if (project.hasFacet(ResourcesFacet.class)) {
            resourcesFacet = project.getFacet(ResourcesFacet.class);
        }
        if (project.hasFacet(WebResourcesFacet.class)) {
            webResourcesFacet = project.getFacet(WebResourcesFacet.class);
        }
        return asRelativeFile(currentFile, javaSourceFacet, resourcesFacet, webResourcesFacet);
    }

    public static String asRelativeFile(String currentFile, JavaSourceFacet javaSourceFacet, ResourcesFacet resourcesFacet, WebResourcesFacet webResourcesFacet) {
        boolean javaFile = currentFile != null && currentFile.endsWith(".java");

        // if its not a java file, then we need to have the relative path name
        String target = null;
        if (javaFile && javaSourceFacet != null) {
            // we only want the relative dir name from the source directory, eg src/main/java
            String baseDir = javaSourceFacet.getSourceDirectory().getFullyQualifiedName();
            String fqn = currentFile;
            if (fqn != null && fqn.startsWith(baseDir) && fqn.length() > baseDir.length()) {
                target = fqn.substring(baseDir.length() + 1);
            }
            // could be in test directory
            if (target == null) {
                // we only want the relative dir name from the source directory, eg src/test/java
                baseDir = javaSourceFacet.getTestSourceDirectory().getFullyQualifiedName();
                fqn = currentFile;
                if (fqn != null && fqn.startsWith(baseDir) && fqn.length() > baseDir.length()) {
                    target = fqn.substring(baseDir.length() + 1);
                }
            }
        } else if (resourcesFacet != null || webResourcesFacet != null) {
            if (resourcesFacet != null) {
                // we only want the relative dir name from the resource directory, eg src/main/resources
                String baseDir = resourcesFacet.getResourceDirectory().getFullyQualifiedName();
                String fqn = currentFile;
                if (fqn != null && fqn.startsWith(baseDir) && fqn.length() > baseDir.length()) {
                    target = fqn.substring(baseDir.length() + 1);
                }
            }
            if (target == null && webResourcesFacet != null) {
                // we only want the relative dir name from the web resource directory, eg WEB-INF/foo.xml
                String baseDir = webResourcesFacet.getWebRootDirectory().getFullyQualifiedName();
                String fqn = currentFile;
                if (fqn != null && fqn.startsWith(baseDir) && fqn.length() > baseDir.length()) {
                    target = fqn.substring(baseDir.length() + 1);
                }
            }
        }

        return target != null ? target : currentFile;
    }

}
