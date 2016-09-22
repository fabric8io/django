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
package io.fabric8;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.jar.AbstractJarMojo;

@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ConnectorMojo extends AbstractJarMojo {

    /**
     * Directory containing the classes and resource files that should be packaged into the JAR.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    @Override
    protected File getClassesDirectory() {
        return classesDirectory;
    }

    @Override
    protected String getClassifier() {
        return "camel-connector";
    }

    @Override
    protected String getType() {
        return "jar";
    }

    @Override
    public File createArchive() throws MojoExecutionException {

        // find the component dependency and get its .json file

        File file = new File(classesDirectory, "camel-connector.json");
        if (file.exists()) {
            try {
                List<String> json = loadFile(file);

                String scheme = extractScheme(json);
                String groupId = extractGroupId(json);
                String artifactId = extractArtifactId(json);
                String version = extractVersion(json); // version not in use

                // find the artifact on the classpath that has the Camel component this connector is using
                // then we want to grab its json schema file to embed in this JAR so we have all files together

                if (scheme != null && groupId != null && artifactId != null) {
                    for (Artifact artifact : getProject().getDependencyArtifacts()) {
                        if ("jar".equals(artifact.getType())) {
                            if (groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId())) {
                                // load the component file inside the file
                                URL url = new URL("file:" + artifact.getFile());
                                URLClassLoader child = new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader());

                                InputStream is = child.getResourceAsStream("META-INF/services/org/apache/camel/component/" + scheme);
                                if (is != null) {
                                    List<String> lines = loadFile(is);
                                    String fqn = extractClass(lines);
                                    is.close();

                                    // only keep package
                                    String pck = fqn.substring(0, fqn.lastIndexOf("."));
                                    String name = pck.replace(".", "/") + "/" + scheme + ".json";

                                    is = child.getResourceAsStream(name);
                                    if (is != null) {
                                        List<String> schema = loadFile(is);
                                        is.close();

                                        // write schema to file
                                        File out = new File(classesDirectory, "camel-component-schema.json");
                                        FileOutputStream fos = new FileOutputStream(out, false);
                                        for (String line : schema) {
                                            fos.write(line.getBytes());
                                            fos.write("\n".getBytes());
                                        }
                                        fos.close();

                                        getLog().info("Embedded camel-component-schema.json file for Camel component " + scheme);
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                throw new MojoExecutionException("Cannot read file camel-connector.json", e);
            }
        }

        return super.createArchive();
    }

    private String extractClass(List<String> lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("class=")) {
                return line.substring(6);
            }
        }
        return null;
    }

    private String extractScheme(List<String> json) {
        for (String line : json) {
            line = line.trim();
            if (line.startsWith("\"scheme\":")) {
                String answer = line.substring(10);
                return answer.substring(0, answer.length() - 2);
            }
        }
        return null;
    }

    private String extractGroupId(List<String> json) {
        for (String line : json) {
            line = line.trim();
            if (line.startsWith("\"groupId\":")) {
                String answer = line.substring(11);
                return answer.substring(0, answer.length() - 2);
            }
        }
        return null;
    }

    private String extractArtifactId(List<String> json) {
        for (String line : json) {
            line = line.trim();
            if (line.startsWith("\"artifactId\":")) {
                String answer = line.substring(14);
                return answer.substring(0, answer.length() - 2);
            }
        }
        return null;
    }

    private String extractVersion(List<String> json) {
        for (String line : json) {
            line = line.trim();
            if (line.startsWith("\"version\":")) {
                String answer = line.substring(11);
                return answer.substring(0, answer.length() - 2);
            }
        }
        return null;
    }

    private List<String> loadFile(File file) throws Exception {
        List<String> lines = new ArrayList<>();
        LineNumberReader reader = new LineNumberReader(new FileReader(file));

        String line;
        do {
            line = reader.readLine();
            if (line != null) {
                lines.add(line);
            }
        } while (line != null);
        reader.close();

        return lines;
    }

    private List<String> loadFile(InputStream fis) throws Exception {
        List<String> lines = new ArrayList<>();
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(fis));

        String line;
        do {
            line = reader.readLine();
            if (line != null) {
                lines.add(line);
            }
        } while (line != null);
        reader.close();

        return lines;
    }
}
