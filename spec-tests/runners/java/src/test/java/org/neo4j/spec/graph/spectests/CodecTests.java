package org.neo4j.spec.graph.spectests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.spec.graph.Format;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.GraphSpecException;
import org.neo4j.spec.graph.migrate.SpecVersion;
import org.neo4j.spec.graph.model.ConstraintType;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.model.Labels;
import org.neo4j.spec.graph.model.Neo4jType;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.NodeDisplay;
import org.neo4j.spec.graph.model.NodeExtensions;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipTarget;
import org.neo4j.spec.graph.model.VectorIndexOptions;

/** Tests decoding and encoding beyond the language-neutral cases. */
class CodecTests {

    private static final Path CASES = Paths.get(System.getProperty("specTests.cases", "../../cases"));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final GraphSpec SPEC = GraphSpec.defaults();
    private static final String V110 = "https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json";
    private static final String V100 = "https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json";

    private static String doc(String nodes) {
        return "{\"schema\":\"" + V110 + "\",\"version\":\"1\",\"id\":\"t\",\"nodes\":{" + nodes + "}}";
    }

    private static String sample() {
        return read(CASES.resolve("documents/sample-retail-banking/shorthand.json"));
    }

    @Test
    void decodesAnOldVersionAndEncodesAtAnyVersion() throws IOException {
        GraphModel model = SPEC.decode(sample(), Format.JSON);
        assertEquals(V110, model.schema());
        assertEquals(V110, tree(SPEC.encode(model, Format.JSON)).get("schema").asText());
        assertEquals(
                V110,
                tree(SPEC.encode(model, Format.JSON, SpecVersion.LATEST)).get("schema").asText());
        String old = SPEC.encode(model, Format.JSON, SpecVersion.V1_0_0);
        assertEquals(V100, tree(old).get("schema").asText());
        assertEquals(model, SPEC.decode(old, Format.JSON));
    }

    @Test
    void decodesWithAnExplicitVersion() {
        GraphModel model = SPEC.decode(sample(), Format.JSON, SpecVersion.V1_0_0);
        assertEquals(SPEC.decode(sample(), Format.JSON), model);
    }

    @Test
    void unknownFieldsAreRejectedWithAPath() {
        String text = doc("\"a\":{\"labels\":{\"identifier\":\"A\"},\"bogus\":1}");
        GraphSpecException e = assertThrows(GraphSpecException.class, () -> SPEC.decode(text, Format.JSON));
        assertEquals("nodes.a.bogus", e.path().orElse(null));
        GraphModel model =
                SPEC.toBuilder().failOnUnknownFields(false).build().decode(text, Format.JSON);
        assertTrue(model.nodes().containsKey("a"));
    }

    @Test
    void customDataIsNotCheckedForUnknownFields() {
        String text = doc("\"a\":{\"extensions\":{\"custom\":{\"anything\":{\"goes\":[1,2.5,\"x\",true]}}}}");
        assertDoesNotThrow(() -> SPEC.decode(text, Format.JSON));
    }

    @Test
    void wrongTypesCarryAPath() {
        assertPath(
                "nodes.a.properties.name.type",
                doc("\"a\":{\"properties\":{\"name\":{\"type\":5}}}"));
        assertPath(
                "nodes.a.properties.name.type",
                doc("\"a\":{\"properties\":{\"name\":{\"type\":\"TEXT\"}}}"));
        assertPath("nodes.a.aliases", doc("\"a\":{\"aliases\":\"x\"}"));
        assertPath("nodes.a.properties.p.dimension", doc("\"a\":{\"properties\":{\"p\":{\"dimension\":\"3\"}}}"));
        assertPath("nodes.a", doc("\"a\":[]"));
        assertPath("nodes.a.aliases[1]", doc("\"a\":{\"aliases\":[\"x\",5]}"));
    }

    @Test
    void missingRequiredFieldsCarryAPath() {
        assertPath("nodes.a.constraints.c.type", doc("\"a\":{\"constraints\":{\"c\":{\"properties\":[\"x\"]}}}"));
        assertPath("id", "{\"schema\":\"" + V110 + "\",\"version\":\"1\"}");
        assertPath(
                "nodes.a.extensions.indexes.i.options.type",
                doc("\"a\":{\"extensions\":{\"indexes\":{\"i\":{\"type\":\"TEXT\",\"labels\":[],\"properties\":[],"
                        + "\"options\":{}}}}}"));
    }

    @Test
    void nullInCustomDataIsRejected() {
        assertPath("nodes.a.extensions.custom.k", doc("\"a\":{\"extensions\":{\"custom\":{\"k\":null}}}"));
    }

    @Test
    void documentsThatAreNotObjectsAreRejected() {
        assertThrows(GraphSpecException.class, () -> SPEC.decode("[]", Format.JSON));
        assertThrows(GraphSpecException.class, () -> SPEC.decode("[]", Format.YAML));
        assertThrows(GraphSpecException.class, () -> SPEC.decode("", Format.JSON));
        assertThrows(GraphSpecException.class, () -> SPEC.decode("{", Format.JSON));
        assertThrows(GraphSpecException.class, () -> SPEC.decode("a: [", Format.YAML));
    }

    @Test
    void readerAndWriterOverloads() {
        GraphModel model = SPEC.decode(new StringReader(sample()), Format.JSON);
        assertEquals(SPEC.decode(sample(), Format.JSON), model);
        assertEquals(model, SPEC.decode(new StringReader(sample()), Format.JSON, SpecVersion.V1_0_0));
        for (Format format : Format.values()) {
            StringWriter writer = new StringWriter();
            SPEC.encode(model, format, SpecVersion.LATEST, writer);
            assertEquals(SPEC.encode(model, format), writer.toString());
        }
    }

    @Test
    void shorthandAddsToLonghandConstraints() {
        GraphModel model = SPEC.decode(
                doc("\"a\":{\"label\":\"A\",\"properties\":{\"id\":{\"key\":true},\"n\":{\"must_exist\":true}},"
                        + "\"constraints\":{\"c\":{\"type\":\"UNIQUE\",\"properties\":[\"n\"]}}}"),
                Format.JSON);
        Node node = model.nodes().get("a");
        assertEquals("A", node.labels().get().identifier().get());
        assertEquals(List.of("c", "key_a_id", "exists_a_n"), new ArrayList<>(node.constraints().keySet()));
        assertEquals("id_A_key", node.constraints().get("key_a_id").name().get());
    }

    @Test
    void emptyAndDefaultValuesMeanAbsent() {
        GraphModel a = SPEC.decode(
                doc("\"a\":{\"labels\":{\"implied\":[],\"extensions\":{\"custom\":{}}},\"aliases\":[],"
                        + "\"extensions\":{\"custom\":{}},\"properties\":{\"p\":{\"type\":\"ANY\",\"must_exist\":false}}}"),
                Format.JSON);
        GraphModel b = SPEC.decode(doc("\"a\":{\"properties\":{\"p\":{}}}"), Format.JSON);
        assertEquals(b, a);
    }

    @Test
    void yamlScalarsStayStringsWhenTheyAreNotJsonLiterals() {
        GraphModel model = SPEC.decode(
                "schema: \"" + V110 + "\"\nversion: 2026.11-rc.1\nid: x\nnodes:\n  n:\n    labels:\n"
                        + "      identifier: Null\n      implied: [Yes, No, True]\n",
                Format.YAML);
        Labels labels = model.nodes().get("n").labels().get();
        assertEquals("Null", labels.identifier().get());
        assertEquals(List.of("Yes", "No", "True"), labels.implied());
        assertEquals("2026.11-rc.1", model.version());
    }

    @Test
    void crossConversionRoundTrips() {
        for (Path dir : caseDirs(CASES.resolve("documents"))) {
            GraphModel fromJson = SPEC.decode(read(dir.resolve("model.json")), Format.JSON);
            GraphModel fromYaml = SPEC.decode(read(dir.resolve("model.yaml")), Format.YAML);
            assertEquals(fromJson, fromYaml, dir.getFileName().toString());
            assertEquals(fromJson, SPEC.decode(SPEC.encode(fromYaml, Format.JSON), Format.JSON));
            assertEquals(fromJson, SPEC.decode(SPEC.encode(fromJson, Format.YAML), Format.YAML));
        }
    }

    @Test
    void jsonOutputMatchesTheCasesByteForByte() {
        for (Path dir : caseDirs(CASES.resolve("documents"))) {
            String expected = read(dir.resolve("model.json"));
            assertEquals(expected, SPEC.encode(SPEC.decode(expected, Format.JSON), Format.JSON), dir.toString());
        }
    }

    @Test
    void builtModelsEncodeAndDecodeToAnEqualModel() {
        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("n", 1);
        custom.put("big", 1L << 40);
        custom.put("d", 0.5);
        custom.put("list", List.of("a", true));
        GraphModel model = GraphModel.builder()
                .schema(V110)
                .version("2")
                .id("built")
                .name("Built")
                .node(
                        "person",
                        Node.builder()
                                .labels(Labels.builder()
                                        .identifier("Person")
                                        .impliedLabel("Agent")
                                        .build())
                                .property("name", Property.builder().type(Neo4jType.STRING).build())
                                .property(
                                        "emb",
                                        Property.builder()
                                                .type(Neo4jType.VECTOR_FLOAT32)
                                                .dimension(3)
                                                .build())
                                .constraint(
                                        "c",
                                        NodeConstraint.builder()
                                                .type(ConstraintType.UNIQUE)
                                                .property("name")
                                                .build())
                                .extensions(NodeExtensions.builder()
                                        .display(NodeDisplay.builder()
                                                .x(1.5)
                                                .y(-2.0)
                                                .build())
                                        .index(
                                                "i",
                                                NodeIndex.builder()
                                                        .type(IndexType.VECTOR)
                                                        .label("Person")
                                                        .property("emb")
                                                        .options(VectorIndexOptions.builder()
                                                                .dimensions(3)
                                                                .defaultSearchExpansionFactor(1.5)
                                                                .build())
                                                        .custom(custom)
                                                        .build())
                                        .build())
                                .build())
                .relationship(
                        "knows",
                        Relationship.builder()
                                .type("KNOWS")
                                .from(RelationshipTarget.builder().node("person").build())
                                .to(RelationshipTarget.builder().node("person").count(2).build())
                                .build())
                .build();
        for (Format format : Format.values()) {
            assertEquals(model, SPEC.decode(SPEC.encode(model, format), format), format.toString());
        }
    }

    @Test
    void unrecognisedSchemaIsPreserved() {
        GraphModel model = SPEC.decode(
                "{\"schema\":\"" + V110 + "\",\"version\":\"1\",\"id\":\"t\"}", Format.JSON);
        GraphModel custom = model.toBuilder().schema("https://example.com/other.json").build();
        assertEquals(
                "https://example.com/other.json",
                tree(SPEC.encode(custom, Format.JSON)).get("schema").asText());
    }

    @TestFactory
    Stream<DynamicTest> validationInputsDecode() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Path set : caseDirs(CASES.resolve("validate"))) {
            for (Path dir : caseDirs(set)) {
                for (String input : List.of("input.json", "input.yaml")) {
                    Path file = dir.resolve(input);
                    Format format = input.endsWith(".json") ? Format.JSON : Format.YAML;
                    tests.add(DynamicTest.dynamicTest(
                            set.getFileName() + "/" + dir.getFileName() + "/" + input,
                            () -> assertDoesNotThrow(() -> SPEC.decode(read(file), format))));
                }
            }
        }
        assertFalse(tests.isEmpty());
        return tests.stream();
    }

    private static void assertPath(String path, String text) {
        GraphSpecException e = assertThrows(GraphSpecException.class, () -> SPEC.decode(text, Format.JSON));
        assertEquals(path, e.path().orElse(null), e.getMessage());
    }

    private static JsonNode tree(String text) {
        try {
            return JSON.readTree(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> caseDirs(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
