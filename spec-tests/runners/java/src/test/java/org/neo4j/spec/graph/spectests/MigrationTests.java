package org.neo4j.spec.graph.spectests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.neo4j.spec.graph.GraphSpecException;
import org.neo4j.spec.graph.migrate.DocumentType;
import org.neo4j.spec.graph.migrate.DocumentVersion;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.migrate.Migrations;
import org.neo4j.spec.graph.migrate.SpecVersion;
import org.neo4j.spec.graph.migrate.TreeMigration;

/** Tests {@link Migrations} directly on Jackson trees. */
class MigrationTests {

    private static final Path CASES = Paths.get(System.getProperty("specTests.cases", "../../cases"));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Migrations MIGRATIONS = Migrations.defaults();

    private static final DocumentType TOY = DocumentType.of("toy");
    private static final DocumentVersion TOY_1 = DocumentVersion.of(TOY, "1.0.0");

    private static ObjectNode sample() throws IOException {
        return (ObjectNode) JSON.readTree(
                Files.readAllBytes(CASES.resolve("documents/sample-retail-banking/shorthand.json")));
    }

    private static ObjectNode withoutSchema(ObjectNode node) {
        ObjectNode copy = node.deepCopy();
        copy.remove("schema");
        return copy;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    private static ObjectNode doc(String schema) {
        ObjectNode node = JSON.createObjectNode();
        node.put("schema", schema);
        return node;
    }

    @Test
    void detectsReleasedVersions() {
        assertEquals(SpecVersion.V1_0_0, MIGRATIONS.detect(doc(SpecVersion.schemaUrl(SpecVersion.V1_0_0))));
        assertEquals(SpecVersion.V1_1_0, MIGRATIONS.detect(doc(SpecVersion.schemaUrl(SpecVersion.V1_1_0))));
    }

    @Test
    void detectsSampleAsOnePointZero() throws IOException {
        assertEquals(SpecVersion.V1_0_0, MIGRATIONS.detect(sample()));
    }

    @Test
    void unknownSchemaUrlIsLatest() {
        assertEquals(SpecVersion.LATEST, MIGRATIONS.detect(doc("https://example.com/other.json")));
        assertEquals(SpecVersion.LATEST, MIGRATIONS.detect(doc("https://neo4j.com/docs/ontology-graph-spec/9.9.9/schema.json")));
    }

    @Test
    void missingOrNonTextSchemaFailsDetection() {
        assertThrows(GraphSpecException.class, () -> MIGRATIONS.detect(JSON.createObjectNode()));
        ObjectNode number = JSON.createObjectNode();
        number.put("schema", 1);
        assertThrows(GraphSpecException.class, () -> MIGRATIONS.detect(number));
    }

    @Test
    void migratesForwardsRewritingOnlySchema() throws IOException {
        ObjectNode input = sample();
        ObjectNode output = MIGRATIONS.migrate(input, SpecVersion.V1_0_0, SpecVersion.V1_1_0);
        assertEquals(SpecVersion.schemaUrl(SpecVersion.V1_1_0), output.get("schema").asText());
        assertEquals(withoutSchema(input), withoutSchema(output));
        assertEquals(fieldNames(input), fieldNames(output));
    }

    @Test
    void migratesBackwardsRewritingOnlySchema() throws IOException {
        ObjectNode input = MIGRATIONS.migrate(sample(), SpecVersion.V1_0_0, SpecVersion.V1_1_0);
        ObjectNode output = MIGRATIONS.migrate(input, SpecVersion.V1_1_0, SpecVersion.V1_0_0);
        assertEquals(SpecVersion.schemaUrl(SpecVersion.V1_0_0), output.get("schema").asText());
        assertEquals(sample(), output);
    }

    @Test
    void addsSchemaFirstWhenAbsent() {
        ObjectNode input = JSON.createObjectNode();
        input.put("a", 1);
        ObjectNode output = MIGRATIONS.migrate(input, SpecVersion.V1_0_0, SpecVersion.V1_1_0);
        assertEquals(List.of("schema", "a"), fieldNames(output));
    }

    @Test
    void doesNotMutateInput() throws IOException {
        ObjectNode input = sample();
        ObjectNode before = input.deepCopy();
        MIGRATIONS.migrate(input, SpecVersion.V1_0_0, SpecVersion.V1_1_0);
        assertEquals(before, input);
    }

    @Test
    void identityPathIsEmptyAndMigrateCopies() throws IOException {
        assertTrue(MIGRATIONS.path(SpecVersion.V1_0_0, SpecVersion.V1_0_0).isEmpty());
        ObjectNode input = sample();
        ObjectNode output = MIGRATIONS.migrate(input, SpecVersion.V1_0_0, SpecVersion.V1_0_0);
        assertEquals(input, output);
        assertTrue(input != output);
    }

    @Test
    void unreachablePathNamesBothVersions() {
        DocumentVersion other = DocumentVersion.of(TOY, "2.0.0");
        GraphSpecException e =
                assertThrows(GraphSpecException.class, () -> MIGRATIONS.path(SpecVersion.V1_0_0, other));
        assertTrue(e.getMessage().contains("graph_spec 1.0.0"), e.getMessage());
        assertTrue(e.getMessage().contains("toy 2.0.0"), e.getMessage());
    }

    @Test
    void defaultsRegisterBothSteps() {
        assertEquals(2, MIGRATIONS.migrations().size());
    }

    @Test
    void laterStepWithSameVersionsReplacesEarlier() {
        Migration custom = new TreeMigration(SpecVersion.V1_0_0, SpecVersion.V1_1_0) {
            @Override
            protected ObjectNode transform(ObjectNode document) {
                document.put("custom", true);
                return document;
            }
        };
        Migrations migrations = MIGRATIONS.toBuilder().add(custom).build();
        assertEquals(2, migrations.migrations().size());
        ObjectNode output = migrations.migrate(JSON.createObjectNode(), SpecVersion.V1_0_0, SpecVersion.V1_1_0);
        assertTrue(output.get("custom").asBoolean());
        assertEquals(2, MIGRATIONS.migrations().size());
    }

    @Test
    void customDocumentTypeMigratesThroughGraphSpec() {
        Migrations migrations = MIGRATIONS.toBuilder()
                .detector(document -> document.has("toy")
                        ? Optional.of(TOY_1)
                        : Optional.<DocumentVersion>empty())
                .add(new TreeMigration(TOY_1, SpecVersion.V1_1_0) {
                    @Override
                    protected ObjectNode transform(ObjectNode document) {
                        ObjectNode result = JSON.createObjectNode();
                        result.put("title", document.get("toy").asText());
                        return result;
                    }
                })
                .add(new TreeMigration(SpecVersion.V1_1_0, TOY_1) {
                    @Override
                    protected ObjectNode transform(ObjectNode document) {
                        ObjectNode result = JSON.createObjectNode();
                        result.put("toy", document.get("title").asText());
                        return result;
                    }
                })
                .build();

        ObjectNode toy = JSON.createObjectNode();
        toy.put("toy", "hello");
        assertEquals(TOY_1, migrations.detect(toy));
        // Graph spec detection still works after the custom detector
        assertEquals(SpecVersion.V1_0_0, migrations.detect(doc(SpecVersion.schemaUrl(SpecVersion.V1_0_0))));

        List<Migration> path = migrations.path(TOY_1, SpecVersion.V1_0_0);
        assertEquals(2, path.size());
        assertEquals(TOY_1, path.get(0).from());
        assertEquals(SpecVersion.V1_1_0, path.get(0).to());
        assertEquals(SpecVersion.V1_1_0, path.get(1).from());
        assertEquals(SpecVersion.V1_0_0, path.get(1).to());

        ObjectNode graph = migrations.migrate(toy, TOY_1, SpecVersion.V1_0_0);
        assertEquals(SpecVersion.schemaUrl(SpecVersion.V1_0_0), graph.get("schema").asText());
        assertEquals("hello", graph.get("title").asText());
        assertEquals(JSON.createObjectNode().put("toy", "hello"), toy);

        ObjectNode back = migrations.migrate(graph, SpecVersion.V1_0_0, TOY_1);
        assertEquals(toy, back);
    }
}
