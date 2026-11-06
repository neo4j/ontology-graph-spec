/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.spec.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The point of this artifact: no Kotlin anywhere in its sources or classes. */
class PurityTest {
    private static final Path GENERATED = Path.of(System.getProperty("generatedSources", "build/generated/java"));
    private static final Path CLASSES = Path.of(System.getProperty("compiledClasses", "build/classes/java/main"));

    @Test
    void sourcesDontReferenceKotlin() throws IOException {
        try (Stream<Path> files = Files.walk(GENERATED)) {
            List<Path> offending = files.filter(p -> p.toString().endsWith(".java")).filter(p -> {
                try {
                    String text = Files.readString(p);
                    return text.contains("import kotlin") || text.contains("kotlinx.") || text.contains("Kt.todo(");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            assertEquals(List.of(), offending);
        }
    }

    @Test
    void noKotlinClassesOrMetadata() throws IOException {
        try (Stream<Path> files = Files.walk(CLASSES)) {
            List<Path> classes = files.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList());
            assertTrue(classes.size() > 100, "expected the generated classes, found " + classes.size());
            for (Path path : classes) {
                String bytes = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.ISO_8859_1);
                assertTrue(!bytes.contains("kotlin/"), path + " references kotlin");
            }
        }
    }

    @Test
    void kotlinIsNotOnTheClasspath() {
        assertTrue(!isLoadable("kotlin.Unit"), "kotlin-stdlib is on the classpath");
        assertTrue(!isLoadable("kotlinx.serialization.KSerializer"), "kotlinx-serialization is on the classpath");
    }

    private static boolean isLoadable(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
