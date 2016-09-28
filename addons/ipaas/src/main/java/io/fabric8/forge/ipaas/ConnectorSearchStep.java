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

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.forge.ipaas.repository.ConnectionRepository;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

@FacetConstraint({ResourcesFacet.class})
public class ConnectorSearchStep extends AbstractIPaaSProjectCommand implements UIWizardStep {

    @Inject
    @WithAttributes(label = "Connector", required = true, description = "The connector to add")
    private UISelectOne<String> connectors;

    @Inject
    ConnectionRepository repository;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        String filter = (String) builder.getUIContext().getAttributeMap().get("filter");

        List<String> values = new ArrayList<>();
        List<ConnectionCatalogDto> dtos = repository.search(filter);
        for (ConnectionCatalogDto dto : dtos) {
            values.add(dto.getName());
        }
        connectors.setValueChoices(values);

        builder.add(connectors);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return null;
    }

}
