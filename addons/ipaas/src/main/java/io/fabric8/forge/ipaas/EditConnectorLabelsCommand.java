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
public class EditConnectorLabelsCommand extends AbstractIPaaSProjectCommand {

    @Inject
    @WithAttributes(label = "Labels", description = "Labels of connector (separate by comma)")
    private UIInput<String> labels;

    private transient ConnectionCatalogDto dto;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(EditConnectorLabelsCommand.class)
                .name("iPaaS: Edit Connector Labels").category(Categories.create(CATEGORY))
                .description("Edits the connector labels");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // load the dto
        dto = loadCamelConnectionDto(getSelectedProject(builder.getUIContext()));

        // set the existing labels as comma separated text
        if (dto != null) {
            String[] text = dto.getLabels();
            if (text != null && text.length > 0) {
                String line = String.join(",", text);
                labels.setValue(line);
            }
        }
        builder.add(labels);
    }

    @Override
    public boolean isEnabled(UIContext context) {
        // TODO: https://github.com/fabric8io/django/issues/79
        return true;
        /*boolean answer = super.isEnabled(context);
        if (answer) {
            FileResource<?> fileResource = getCamelConnectorFile(context);
            answer = fileResource != null && fileResource.exists();
        }
        return answer;*/
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String text = labels.getValue();

        if (text != null && !text.isEmpty()) {
            dto.setLabels(text.split(","));
        } else {
            dto.setLabels(null);
        }

        // marshal DTO
        String json = toJson(dto);

        // write the connector json file
        FileResource<?> fileResource = getCamelConnectorFile(context);
        fileResource.setContents(json);

        return Results.success("Updated labels on Connector: " + dto.getName());
    }

}
