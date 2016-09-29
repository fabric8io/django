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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.jboss.forge.addon.dependencies.Dependency;

public class CamelComponentDependencyHelper {

    public static Properties loadComponentProperties(Dependency dependency) {
        Properties answer = new Properties();

        try {
            // is it a JAR file
            File file = dependency.getArtifact().getUnderlyingResourceObject();
            if (file != null && file.getName().toLowerCase().endsWith(".jar")) {
                URL url = new URL("file:" + file.getAbsolutePath());
                URLClassLoader child = new URLClassLoader(new URL[]{url});

                InputStream is = child.getResourceAsStream("META-INF/services/org/apache/camel/component.properties");
                if (is != null) {
                    answer.load(is);
                }
            }
        } catch (Throwable e) {
            // ignore
        }

        return answer;
    }

    public static String extractComponentJavaType(Dependency dependency, String scheme) {
        try {
            // is it a JAR file
            File file = dependency.getArtifact().getUnderlyingResourceObject();
            if (file != null && file.getName().toLowerCase().endsWith(".jar")) {
                URL url = new URL("file:" + file.getAbsolutePath());
                URLClassLoader child = new URLClassLoader(new URL[]{url});

                InputStream is = child.getResourceAsStream("META-INF/services/org/apache/camel/component/" + scheme);
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    return (String) props.get("class");
                }
            }
        } catch (Throwable e) {
            // ignore
        }

        return null;
    }

}
