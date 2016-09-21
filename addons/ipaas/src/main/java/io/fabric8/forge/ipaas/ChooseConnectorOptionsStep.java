/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.ipaas;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

import static io.fabric8.forge.addon.utils.OutputFormatHelper.toJson;

public class ChooseConnectorOptionsStep extends AbstractIPaaSProjectCommand implements UIWizardStep {

    private final String connectorName;
    private final String group;
    private final List<InputComponent> allInputs;
    private final List<InputComponent> inputs;
    private final boolean last;
    private final int index;
    private final int total;

    public ChooseConnectorOptionsStep(ProjectFactory projectFactory,
                                      String connectorName, String group,
                                      List<InputComponent> allInputs,
                                      List<InputComponent> inputs,
                                      boolean last, int index, int total) {
        this.projectFactory = projectFactory;
        this.connectorName = connectorName;
        this.group = group;
        this.allInputs = allInputs;
        this.inputs = inputs;
        this.last = last;
        // we want to 1-based
        this.index = index + 1;
        this.total = total;
    }

    public String getGroup() {
        return group;
    }

    public int getIndex() {
        return index;
    }

    public int getTotal() {
        return total;
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ChooseConnectorOptionsStep.class).name(
                "iPaaS: Choose options").category(Categories.create(CATEGORY))
                .description(String.format("Configure %s options (%s of %s)", group, index, total));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initializeUI(UIBuilder builder) throws Exception {
        if (inputs != null) {
            for (InputComponent input : inputs) {
                builder.add(input);
            }
        }
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();

        // only execute if we are last
        if (last) {
            Set<String> chosenOptions = new LinkedHashSet<>();

            // pickup the chosen values
            for (InputComponent input : allInputs) {
                String key = input.getName();
                // only use the value if a value was set (and the value is not the same as the default value)
                if (input.hasValue()) {
                    String value = input.getValue().toString();
                    if (value != null) {
                        if ("true".equals(value)) {
                            chosenOptions.add(key);
                        }
                    }
                }
            }

            // load the dto
            ConnectionCatalogDto dto = loadCamelConnectionDto(getSelectedProject(context));
            if (dto != null) {
                if (chosenOptions.isEmpty()) {
                    dto.setEndpointOptions(null);
                } else {
                    String[] values = chosenOptions.toArray(new String[chosenOptions.size()]);
                    dto.setEndpointOptions(values);
                }
            }

            // marshal DTO
            String json = toJson(dto);

            // write the connector json file
            FileResource<?> fileResource = getCamelConnectorFile(context);
            fileResource.setContents(json);

            Results.success("Updated chosen options on connector " + dto.getName());
        }

        return null;
    }

}
