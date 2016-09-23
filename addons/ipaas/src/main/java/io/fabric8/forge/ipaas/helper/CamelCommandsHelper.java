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
package io.fabric8.forge.ipaas.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.fabric8.forge.ipaas.model.InputOptionByGroup;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.shell.ui.ShellContext;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.roaster.model.util.Strings;

import static io.fabric8.forge.addon.utils.UIHelper.createUIInput;
import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.endpointComponentName;

public class CamelCommandsHelper {

    public static List<InputOptionByGroup> createUIInputsForCamelComponent(String camelComponentName, Map<String, String> currentValues, int maxOptionsPerPage,
                                                                           boolean chooseOnly, Set<String> chosenOptions, boolean disableRequired,
                                                                           CamelCatalog camelCatalog, InputComponentFactory componentFactory, ConverterFactory converterFactory, UIContext ui) throws Exception {
        return doCreateUIInputsForCamel(camelComponentName, currentValues, maxOptionsPerPage, false, false, chooseOnly, chosenOptions, disableRequired,
                camelCatalog, componentFactory, converterFactory, ui, false);
    }

    public static List<InputOptionByGroup> createUIInputsForCamelEndpoint(String camelComponentName, Map<String, String> currentValues, String uri, int maxOptionsPerPage, boolean consumerOnly, boolean producerOnly,
                                                                          boolean chooseOnly, Set<String> chosenOptions, boolean disableRequired,
                                                                          CamelCatalog camelCatalog, InputComponentFactory componentFactory, ConverterFactory converterFactory, UIContext ui) throws Exception {

        if (camelComponentName == null && uri != null) {
            camelComponentName = endpointComponentName(uri);
        }
        if (currentValues == null) {
            currentValues = uri != null ? camelCatalog.endpointProperties(uri) : Collections.EMPTY_MAP;
        }
        return doCreateUIInputsForCamel(camelComponentName, currentValues, maxOptionsPerPage, consumerOnly, producerOnly, chooseOnly, chosenOptions, disableRequired,
                camelCatalog, componentFactory, converterFactory, ui, true);
    }

    private static List<InputOptionByGroup> doCreateUIInputsForCamel(String camelComponentName, Map<String, String> currentValues, int maxOptionsPerPage, boolean consumerOnly, boolean producerOnly,
                                                                     boolean chooseOnly, Set<String> chosenOptions, boolean disableRequired,
                                                                     CamelCatalog camelCatalog, InputComponentFactory componentFactory, ConverterFactory converterFactory, UIContext ui, boolean endpoint) throws Exception {

        List<InputOptionByGroup> answer = new ArrayList<>();

        String json = camelCatalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }

        // is the component consumer or producer only, if so we do not need any kind of filter
        boolean componentConsumerOnly = CamelCatalogHelper.isComponentConsumerOnly(camelCatalog, camelComponentName);
        boolean componentProducerOnly = CamelCatalogHelper.isComponentProducerOnly(camelCatalog, camelComponentName);
        if (componentConsumerOnly || componentProducerOnly) {
            // reset the filters as the component can only be one of them anyway, so we should show all options
            consumerOnly = false;
            producerOnly = false;
        }


        List<Map<String, String>> data;
        if (endpoint) {
            data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        } else {
            data = JSonSchemaHelper.parseJsonSchema("componentProperties", json, true);
        }

        if (data != null) {

            // whether to prompt for all fields or not in the interactive mode
            boolean promptInInteractiveMode = false;
            if (ui instanceof ShellContext) {
                // we want to prompt if the command was executed without any arguments
                boolean params = ((ShellContext) ui).getCommandLine().hasParameters();
                promptInInteractiveMode = !params;
            }

            List<InputComponent> inputs = new ArrayList<>();
            InputOptionByGroup current = new InputOptionByGroup();
            current.setGroup(null);
            current.setInputs(inputs);

            Set<String> namesAdded = new HashSet<>();

            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String kind = propertyMap.get("kind");
                String group = propertyMap.get("group");
                String label = propertyMap.get("label");
                String type = propertyMap.get("type");
                String javaType = propertyMap.get("javaType");
                String deprecated = propertyMap.get("deprecated");
                String required = propertyMap.get("required");
                String currentValue = currentValues.get(name);
                String defaultValue = propertyMap.get("defaultValue");
                String description = propertyMap.get("description");
                String enums = propertyMap.get("enum");
                String prefix = propertyMap.get("prefix");
                String multiValue = propertyMap.get("multiValue");

                if (current.getGroup() == null) {
                    current.setGroup(group);
                }
                // its a new group
                if (group != null && !group.equals(current.getGroup())) {
                    if (!current.getInputs().isEmpty()) {
                        // this group is now done so add to answer
                        answer.add(current);
                    }

                    // get ready for a new group
                    inputs = new ArrayList<>();
                    current = new InputOptionByGroup();
                    current.setGroup(group);
                    current.setInputs(inputs);
                }

                // filter out options in case we should only include consumers or producers only
                if (consumerOnly && label != null) {
                    if (!label.contains("consumer")) {
                        continue;
                    }
                }
                if (producerOnly && label != null) {
                    if (!label.contains("producer")) {
                        continue;
                    }
                }

                if (!Strings.isNullOrEmpty(name)) {

                    // if choose only then force using a boolean type as we are use the UIInput to select/unselect the option
                    if (chooseOnly) {
                        type = "boolean";
                        javaType = "java.lang.Boolean";
                        enums = null;
                        defaultValue = null;
                        multiValue = null;
                        boolean isChosen = chosenOptions.contains(name);
                        currentValue = isChosen ? "true" : "false";
                        prefix = null;
                    }
                    // you can disable required for example when editing default values to not be forced to enter a value by forge
                    if (disableRequired) {
                        required = "false";
                    }

                    Class<Object> inputClazz = CamelCommandsHelper.loadValidInputTypes(javaType, type);
                    if (inputClazz != null) {
                        if (namesAdded.add(name)) {

                            // if its an enum and its optional then make sure there is a default value
                            // if no default value exists then add none as the 1st choice default value
                            // otherwise the GUI makes us force to select an option which is not what we want
                            if (enums != null && (required == null || "false".equals(required))) {
                                if (defaultValue == null || defaultValue.isEmpty()) {
                                    defaultValue = "none";
                                    if (!enums.startsWith("none,")) {
                                        enums = "none," + enums;
                                    }
                                }
                            }

                            boolean multi = "true".equals(multiValue);
                            InputComponent input = createUIInput(ui.getProvider(), componentFactory, converterFactory, null, name, inputClazz, required, currentValue, defaultValue, enums, description, promptInInteractiveMode, multi, prefix);
                            if (input != null) {
                                inputs.add(input);

                                // if we hit max options then create a new group
                                if (inputs.size() == maxOptionsPerPage) {
                                    // this group is now done so add to answer
                                    if (!current.getInputs().isEmpty()) {
                                        answer.add(current);
                                    }
                                    // get ready for a new group
                                    inputs = new ArrayList<>();
                                    current = new InputOptionByGroup();
                                    current.setGroup(group);
                                    current.setInputs(inputs);
                                }
                            }
                        }
                    }
                }
            }

            // add last group if there was some new inputs
            if (!inputs.isEmpty()) {
                answer.add(current);
            }
        }

        // use common as faullback group name
        for (InputOptionByGroup group : answer) {
            if (group.getGroup() == null) {
                group.setGroup("common");
            }
        }

        return answer;
    }

    /**
     * Converts a java type as a string to a valid input type and returns the class or null if its not supported
     */
    public static Class<Object> loadValidInputTypes(String javaType, String type) {
        // we have generics in the javatype, if so remove it so its loadable from a classloader
        int idx = javaType.indexOf('<');
        if (idx > 0) {
            javaType = javaType.substring(0, idx);
        }

        try {
            Class<Object> clazz = getPrimitiveWrapperClassType(type);
            if (clazz == null) {
                clazz = loadPrimitiveWrapperType(javaType);
            }
            if (clazz == null) {
                clazz = loadStringSupportedType(javaType);
            }
            if (clazz == null) {
                try {
                    clazz = (Class<Object>) Class.forName(javaType);
                } catch (Throwable e) {
                    // its a custom java type so use String as the input type, so you can refer to it using # lookup
                    if ("object".equals(type)) {
                        clazz = loadPrimitiveWrapperType("java.lang.String");
                    }
                }
            }

            // favor specialized UI for these types
            if (clazz != null && (clazz.equals(String.class) || clazz.equals(Date.class) || clazz.equals(Boolean.class)
                    || clazz.isPrimitive() || Number.class.isAssignableFrom(clazz))) {
                return clazz;
            }

            // its a custom java type so use String as the input type, so you can refer to it using # lookup
            if ("object".equals(type)) {
                clazz = loadPrimitiveWrapperType("java.lang.String");
                return clazz;
            }

        } catch (Throwable e) {
            // ignore errors
        }
        return null;
    }

    private static Class loadStringSupportedType(String javaType) {
        if ("java.io.File".equals(javaType)) {
            return String.class;
        } else if ("java.net.URL".equals(javaType)) {
            return String.class;
        } else if ("java.net.URI".equals(javaType)) {
            return String.class;
        }
        return null;
    }

    /**
     * Gets the JSon schema primitive type.
     *
     * @param name the json type
     * @return the primitive Java Class type
     */
    public static Class getPrimitiveWrapperClassType(String name) {
        if ("string".equals(name)) {
            return String.class;
        } else if ("boolean".equals(name)) {
            return Boolean.class;
        } else if ("integer".equals(name)) {
            return Integer.class;
        } else if ("number".equals(name)) {
            return Float.class;
        }

        return null;
    }

    private static Class loadPrimitiveWrapperType(String name) {
        // special for byte[] or Object[] as its common to use
        if ("java.lang.byte[]".equals(name) || "byte[]".equals(name)) {
            return Byte[].class;
        } else if ("java.lang.Byte[]".equals(name) || "Byte[]".equals(name)) {
            return Byte[].class;
        } else if ("java.lang.Object[]".equals(name) || "Object[]".equals(name)) {
            return Object[].class;
        } else if ("java.lang.String[]".equals(name) || "String[]".equals(name)) {
            return String[].class;
            // and these is common as well
        } else if ("java.lang.String".equals(name) || "String".equals(name)) {
            return String.class;
        } else if ("java.lang.Boolean".equals(name) || "Boolean".equals(name)) {
            return Boolean.class;
        } else if ("boolean".equals(name)) {
            return Boolean.class;
        } else if ("java.lang.Integer".equals(name) || "Integer".equals(name)) {
            return Integer.class;
        } else if ("int".equals(name)) {
            return Integer.class;
        } else if ("java.lang.Long".equals(name) || "Long".equals(name)) {
            return Long.class;
        } else if ("long".equals(name)) {
            return Long.class;
        } else if ("java.lang.Short".equals(name) || "Short".equals(name)) {
            return Short.class;
        } else if ("short".equals(name)) {
            return Short.class;
        } else if ("java.lang.Byte".equals(name) || "Byte".equals(name)) {
            return Byte.class;
        } else if ("byte".equals(name)) {
            return Byte.class;
        } else if ("java.lang.Float".equals(name) || "Float".equals(name)) {
            return Float.class;
        } else if ("float".equals(name)) {
            return Float.class;
        } else if ("java.lang.Double".equals(name) || "Double".equals(name)) {
            return Double.class;
        } else if ("double".equals(name)) {
            return Double.class;
        } else if ("java.lang.Character".equals(name) || "Character".equals(name)) {
            return Character.class;
        } else if ("char".equals(name)) {
            return Character.class;
        }
        return null;
    }

    public static String asJavaClassName(String name) {
        name = dashToCamelCase(name);
        return Strings.capitalize(name);
    }

    public static String asSchemeName(String name) {
        name = camelCaseToDash(name);
        // skip first dash
        if (name.startsWith("-")) {
            name = name.substring(1);
        }
        // skip last dash
        if (name.endsWith("-")) {
            name = name.substring(0, name.length() - 1);
        }
        // lower case all
        name = name.toLowerCase(Locale.US);
        return name;
    }

    private static String dashToCamelCase(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        boolean upper = false;

        for (char c : value.toCharArray()) {
            if (c == '-') {
                upper = true;
                continue;
            }
            if (upper) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
            upper = false;
        }
        return sb.toString();
    }

    public static String camelCaseToDash(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        boolean dash = false;

        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) {
                dash = true;
            }
            if (dash) {
                sb.append('-');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
            dash = false;
        }
        return sb.toString();
    }


}
