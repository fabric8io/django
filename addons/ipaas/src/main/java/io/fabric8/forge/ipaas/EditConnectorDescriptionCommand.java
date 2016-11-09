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

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.facets.MavenDependencyFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import static io.fabric8.forge.addon.utils.OutputFormatHelper.toJson;

@FacetConstraint({MavenDependencyFacet.class})
public class EditConnectorDescriptionCommand extends AbstractIPaaSProjectCommand {

    @Inject
    @WithAttributes(label = "Description", required = true, description = "Description of connector")
    private UIInput<String> description;

    private transient ConnectionCatalogDto dto;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(EditConnectorDescriptionCommand.class)
                .name("iPaaS: Edit Connector Description").category(Categories.create(CATEGORY))
                .description("Edits the connector description");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // load the dto
        dto = loadCamelConnectionDto(getSelectedProject(builder.getUIContext()));

        // set the existing description
        if (dto != null) {
            String text = dto.getDescription();
            if (text != null) {
                description.setValue(text);
            }
        }
        builder.add(description);
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean answer = super.isEnabled(context);
        if (answer) {
            FileResource<?> fileResource = getCamelConnectorFile(context);
            answer = fileResource != null && fileResource.exists();
        }
        return answer;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String text = description.getValue();

        dto.setDescription(text);

        // marshal DTO
        String json = toJson(dto);

        // write the connector json file
        FileResource<?> fileResource = getCamelConnectorFile(context);
        fileResource.setContents(json);

        return Results.success("Updated description on Connector: " + dto.getName());
    }

}
