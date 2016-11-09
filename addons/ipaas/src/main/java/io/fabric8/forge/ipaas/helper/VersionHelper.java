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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.loadText;

public final class VersionHelper {

    private static volatile String version;

    public synchronized String getVersion() {
        if (version != null) {
            return version;
        }
        InputStream is = null;
        // try to load from maven properties first
        try {
            Properties p = new Properties();
            is = getClass().getResourceAsStream("/META-INF/maven/io.fabric8.django/ipaas/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = getClass().getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "";
        }

        return version;
    }

    public static String fabric8MavenPluginVersion() {
        InputStream is = VersionHelper.class.getResourceAsStream("/META-INF/maven/io.fabric8.django/ipaas/pom.xml");
        try {
            String text = loadText(is);
            return between(text, "<fabric8.maven.plugin.version>", "</fabric8.maven.plugin.version>");
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    private static String between(String text, String after, String before) {
        text = after(text, after);
        if (text == null) {
            return null;
        }
        return before(text, before);
    }

    private static String after(String text, String after) {
        if (!text.contains(after)) {
            return null;
        }
        return text.substring(text.indexOf(after) + after.length());
    }

    private static String before(String text, String before) {
        if (!text.contains(before)) {
            return null;
        }
        return text.substring(0, text.indexOf(before));
    }

}
