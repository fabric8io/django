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
package io.fabric8.forge.ipaas.utils;

import org.jboss.forge.addon.parser.java.ui.validators.AbstractJLSUIValidator;
import org.jboss.forge.addon.parser.java.utils.ResultType;
import org.jboss.forge.addon.parser.java.utils.ValidationResult;

public class ConnectorNameValidator extends AbstractJLSUIValidator {

    @Override
    protected ValidationResult validate(String s) {
        if (s == null) {
            return new ValidationResult(ResultType.INFO);
        }

        // must only be chars or a dash
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (ch == '-' || Character.isLetter(ch)) {
                continue;
            }
            return new ValidationResult(ResultType.ERROR, "The connector name must be A..Z or - only characters.");
        }

        return new ValidationResult(ResultType.INFO);
    }
}

