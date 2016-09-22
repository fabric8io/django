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
package io.fabric8.forge.ipaas.projecttypes;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.forge.ipaas.CreateConnectorCommand;
import org.jboss.forge.addon.maven.projects.archetype.CustomMavenArchetypeProjectType;
import org.jboss.forge.addon.parser.java.facets.JavaCompilerFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.PackagingFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Results;

/**
 * A project type for Camel Connector projects
 */
public class ConnectorProjectType extends CustomMavenArchetypeProjectType {

    public ConnectorProjectType() {
        super(ConnectorProjectArchetypeStep.class, "Connector");
    }

    @Override
    public Iterable<Class<? extends ProjectFacet>> getRequiredFacets() {
        List<Class<? extends ProjectFacet>> result = new ArrayList<Class<? extends ProjectFacet>>(6);
        result.add(MetadataFacet.class);
        result.add(PackagingFacet.class);
        result.add(DependencyFacet.class);
        result.add(ResourcesFacet.class);
        result.add(JavaSourceFacet.class);
        result.add(JavaCompilerFacet.class);
        return result;
    }

    @Override
    public NavigationResult next(UINavigationContext context) {
        return Results.navigateTo(this.getSetupFlow(), CreateConnectorCommand.class);
    }

    @Override
    public int priority() {
        return super.priority() + 9001;
    }

}
