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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import org.apache.camel.catalog.CamelCatalog;
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
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.getPrefix;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.isDefaultValue;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.isMultiValue;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.isNonePlaceholderEnumValue;

public class EditComponentOptionsStep extends AbstractIPaaSProjectCommand implements UIWizardStep {

    private final String connectorName;
    private final String camelComponentName;
    private final String group;
    private final List<InputComponent> allInputs;
    private final List<InputComponent> inputs;
    private final boolean last;
    private final int index;
    private final int total;
    private final CamelCatalog camelCatalog;

    public EditComponentOptionsStep(ProjectFactory projectFactory,
                                    CamelCatalog camelCatalog,
                                    String connectorName, String camelComponentName, String group,
                                    List<InputComponent> allInputs,
                                    List<InputComponent> inputs,
                                    boolean last, int index, int total) {
        this.projectFactory = projectFactory;
        this.camelCatalog = camelCatalog;
        this.connectorName = connectorName;
        this.camelComponentName = camelComponentName;
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
        return Metadata.forCommand(EditComponentOptionsStep.class).name(
                "iPaaS: Edit options").category(Categories.create(CATEGORY))
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
        // only execute if we are last
        if (last) {
            // collect all the options that was set
            Map<String, String> options = new LinkedHashMap<>();
            for (InputComponent input : allInputs) {
                String key = input.getName();
                // only use the value if a value was set (and the value is not the same as the default value)
                if (input.hasValue()) {
                    String value = input.getValue().toString();
                    if (value != null) {
                        // special for multivalued options
                        boolean isMultiValue = isMultiValue(camelCatalog, camelComponentName, key);
                        if (isMultiValue) {
                            String prefix = getPrefix(camelCatalog, camelComponentName, key);
                            // ensure the value has prefix for all its options
                            // rebuild value (accordingly to above comment)
                            StringBuilder sb = new StringBuilder();
                            String[] parts = value.split("&amp;");
                            for (int i = 0; i < parts.length; i++) {
                                String part = parts[i];
                                if (!part.startsWith(prefix)) {
                                    part = prefix + part;
                                }
                                sb.append(part);
                                if (i < parts.length - 1) {
                                    sb.append("&");
                                }
                            }
                            value = sb.toString();
                        }

                        boolean matchDefault = isDefaultValue(camelCatalog, camelComponentName, key, value);
                        if ("none".equals(value)) {
                            // special for enum that may have a none as dummy placeholder which we should not add
                            boolean nonePlaceholder = isNonePlaceholderEnumValue(camelCatalog, camelComponentName, key);
                            if (!matchDefault && !nonePlaceholder) {
                                options.put(key, value);
                            }
                        } else if (!matchDefault) {
                            options.put(key, value);
                        }
                    }
                }
            }

            // load the dto
            ConnectionCatalogDto dto = loadCamelConnectionDto(getSelectedProject(context));
            if (dto != null) {
                if (options.isEmpty()) {
                    dto.setComponentValues(null);
                } else {
                    dto.setComponentValues(options);
                }
            }

            // marshal DTO
            String json = toJson(dto);

            // write the connector json file
            FileResource<?> fileResource = getCamelConnectorFile(context);
            fileResource.setContents(json);

            Results.success("Updated default values on Component: " + dto.getName());
        }

        return null;
    }

}
