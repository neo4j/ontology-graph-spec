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
package org.neo4j.spec.graph.codec.format;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.neo4j.spec.graph.model.GraphModel;

/**
 * {@link ModelCodec} maps every model field to a JSON key by hand, so adding a field to the model without touching the
 * codec would silently drop it. This test reflects over every model class and checks that the codec mentions the key of
 * each of its fields, both when encoding and when decoding.
 *
 * <p>The check is deliberately coarse: it looks for the key among the string literals of the codec's compiled classes
 * rather than checking which type's method uses it, so it catches a forgotten field but not a misplaced one. Behaviour
 * is covered by the spec tests.
 */
class ModelCodecFieldsTest {
    private static final String MODEL_PACKAGE = GraphModel.class.getPackageName();

    /** Model classes whose fields are not part of the serialised format. */
    private static final Set<String> NOT_SERIALISED = Set.of(
            // Utility and metadata classes
            "org.neo4j.spec.graph.model.Version",
            "org.neo4j.spec.graph.model.NameFormat",
            "org.neo4j.spec.graph.model.Notation");

    /**
     * Fields whose key isn't simply the snake_case of the field name, as {@code Class.field -> key}. Every other field
     * is expected under its snake_case name.
     */
    private static final Map<String, String> KEYS = new HashMap<>();

    static {
        String fullText = "FullTextIndexOption.";
        KEYS.put(fullText + "defaultAnalyzer", "fulltext.default_analyzer");
        KEYS.put(fullText + "analyzer", "fulltext.analyzer");
        KEYS.put(fullText + "eventuallyConsistent", "fulltext.eventually_consistent");
        KEYS.put(fullText + "eventuallyConsistentApplyParallelism", "fulltext.eventually_consistent_apply_parallelism");
        KEYS.put(fullText + "eventuallyConsistentRefreshInterval", "fulltext.eventually_consistent_refresh_interval");
        KEYS.put(
                fullText + "eventuallyConsistentRefreshParallelism",
                "fulltext.eventually_consistent_refresh_parallelism");
        KEYS.put(
                fullText + "eventuallyConsistentUpdateQueueMaxLength",
                "fulltext.eventually_consistent_index_update_queue_max_length");

        String point = "PointIndexOption.";
        KEYS.put(point + "cartesianMin", "spatial.cartesian.min");
        KEYS.put(point + "cartesianMax", "spatial.cartesian.max");
        KEYS.put(point + "cartesian3DMin", "spatial.cartesian-3d.min");
        KEYS.put(point + "cartesian3DMax", "spatial.cartesian-3d.max");
        KEYS.put(point + "wgs84Min", "spatial.wgs-84.min");
        KEYS.put(point + "wgs84Max", "spatial.wgs-84.max");
        KEYS.put(point + "wgs843DMin", "spatial.wgs-84-3d.min");
        KEYS.put(point + "wgs843DMax", "spatial.wgs-84-3d.max");

        String vector = "VectorIndexOption.";
        KEYS.put(vector + "dimensions", "vector.dimensions");
        KEYS.put(vector + "similarityFunction", "vector.similarity_function");
        KEYS.put(vector + "defaultSearchExpansionFactor", "vector.default_search_expansion_factor");
        KEYS.put(vector + "quantizationEnabled", "vector.quantization.enabled");
        KEYS.put(vector + "quantizationType", "vector.quantization.type");
        KEYS.put(vector + "hnswM", "vector.hnsw.m");
        KEYS.put(vector + "hnswEfConstruction", "vector.hnsw.ef_construction");
    }

    @Test
    void everyModelFieldIsEncoded() throws Exception {
        assertCovered("encode", stringLiterals("ModelCodec"));
    }

    @Test
    void everyModelFieldIsDecoded() throws Exception {
        Set<String> literals = stringLiterals("ModelCodec$Decoder");
        literals.addAll(stringLiterals("ModelCodec$Decoder$Fields"));
        assertCovered("decode", literals);
    }

    @Test
    void modelClassesAreDiscovered() throws Exception {
        // Guards against the discovery silently finding nothing, which would make the tests above pass vacuously
        assertTrue(modelClasses().size() > 20, "Found only " + modelClasses());
    }

    private static void assertCovered(String direction, Set<String> literals) throws Exception {
        List<String> missing = new ArrayList<>();
        for (Class<?> type : modelClasses()) {
            for (Field field : serialisedFields(type)) {
                String key = key(type, field);
                if (!literals.contains(key)) {
                    missing.add(type.getSimpleName() + "." + field.getName() + " (expected key '" + key + "')");
                }
            }
        }
        assertTrue(
                missing.isEmpty(),
                "ModelCodec does not " + direction + " these model fields. Update it, or if the key differs from the"
                        + " snake_case field name add it to KEYS in this test:\n  " + String.join("\n  ", missing));
    }

    private static String key(Class<?> type, Field field) {
        String name = field.getName();
        return KEYS.getOrDefault(type.getSimpleName() + "." + name, snakeCase(name));
    }

    private static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    /** The instance fields declared by the class and its superclasses. */
    private static List<Field> serialisedFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                fields.add(field);
            }
        }
        return fields;
    }

    /** Every top-level, non-enum model class with at least one instance field. */
    private static List<Class<?>> modelClasses() throws Exception {
        Path root = classesRoot();
        Path packageDir = root.resolve(MODEL_PACKAGE.replace('.', '/'));
        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(packageDir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".class") || fileName.contains("$")) continue;
                String className = root.relativize(file)
                        .toString()
                        .replace(file.getFileSystem().getSeparator(), ".")
                        .replaceAll("\\.class$", "");
                if (NOT_SERIALISED.contains(className)) continue;
                Class<?> type = Class.forName(className);
                if (type.isEnum() || type.isInterface() || type.isAnnotation()) continue;
                if (serialisedFields(type).isEmpty()) continue;
                classes.add(type);
            }
        }
        return classes;
    }

    private static Path classesRoot() throws URISyntaxException {
        return Paths.get(GraphModel.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
    }

    /** The string constants (as loaded by {@code ldc}) of a class in the codec's package, read from its class file. */
    private static Set<String> stringLiterals(String className) throws IOException {
        try (InputStream in = ModelCodec.class.getResourceAsStream(className + ".class")) {
            if (in == null) throw new IOException("Cannot find the class file of " + className);
            return stringLiterals(new DataInputStream(in));
        }
    }

    private static Set<String> stringLiterals(DataInputStream in) throws IOException {
        in.readInt(); // magic
        in.readUnsignedShort(); // minor
        in.readUnsignedShort(); // major
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        Set<Integer> stringIndexes = new HashSet<>();
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1:
                    utf8[i] = in.readUTF();
                    break;
                case 8:
                    stringIndexes.add(in.readUnsignedShort());
                    break;
                case 7:
                case 16:
                case 19:
                case 20:
                    in.skipBytes(2);
                    break;
                case 15:
                    in.skipBytes(3);
                    break;
                case 3:
                case 4:
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    in.skipBytes(4);
                    break;
                case 5:
                case 6:
                    in.skipBytes(8);
                    i++; // Longs and doubles take two slots
                    break;
                default:
                    throw new IOException("Unknown constant pool tag " + tag);
            }
        }
        Set<String> literals = new TreeSet<>();
        for (int index : stringIndexes) literals.add(utf8[index]);
        return literals;
    }
}
