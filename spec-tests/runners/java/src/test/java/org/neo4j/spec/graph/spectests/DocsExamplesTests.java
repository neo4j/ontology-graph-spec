package org.neo4j.spec.graph.spectests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neo4j.spec.graph.Format;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.migrate.DocumentType;
import org.neo4j.spec.graph.migrate.DocumentVersion;
import org.neo4j.spec.graph.migrate.Migrations;
import org.neo4j.spec.graph.migrate.SpecVersion;
import org.neo4j.spec.graph.migrate.TreeMigration;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Labels;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;
import org.neo4j.spec.graph.validate.ValidatorSet;
import org.neo4j.spec.graph.validate.Validators;
import org.neo4j.spec.graph.validate.core.CoreValidators;

/** The examples from docs/modules/libraries/pages/java.adoc, kept compiling and passing. */
class DocsExamplesTests {

    private static final Path EXAMPLES = Paths.get(System.getProperty("specTests.cases", "../../cases"))
            .resolve("../../docs/modules/ROOT/examples");

    private static String read(String name) throws IOException {
        return new String(Files.readAllBytes(EXAMPLES.resolve(name)), StandardCharsets.UTF_8);
    }

    @Test
    void readAndWrite() throws IOException {
        String yaml = read("movies.yaml");
        GraphSpec spec = GraphSpec.defaults();
        GraphModel model = spec.decode(yaml, Format.YAML);
        String json = spec.encode(model, Format.JSON);
        assertEquals(model, spec.decode(json, Format.JSON));

        GraphModel withPerson = model.toBuilder()
                .node(
                        "person",
                        Node.builder()
                                .labels(Labels.builder().identifier("Person").build())
                                .build())
                .build();
        assertTrue(withPerson.nodes().containsKey("person"));
    }

    @Test
    void versions() throws IOException {
        GraphSpec spec = GraphSpec.defaults();
        String json = read("movies.json");
        GraphModel detected = spec.decode(json, Format.JSON);
        GraphModel given = spec.decode(json, Format.JSON, SpecVersion.V1_0_0);
        String latest = spec.encode(detected, Format.YAML);
        String older = spec.encode(detected, Format.YAML, SpecVersion.V1_0_0);
        assertEquals(detected.nodes(), given.nodes());
        assertTrue(latest.contains(SpecVersion.schemaUrl(SpecVersion.LATEST)));
        assertTrue(older.contains(SpecVersion.schemaUrl(SpecVersion.V1_0_0)));
    }

    @Test
    void anotherFormat() throws IOException {
        DocumentVersion toyV1 = DocumentVersion.of(DocumentType.of("toy"), "1.0.0");
        GraphSpec spec = GraphSpec.builder()
                .migrations(Migrations.defaults()
                        .toBuilder()
                        .add(new TreeMigration(toyV1, SpecVersion.LATEST) {
                            @Override
                            protected ObjectNode transform(ObjectNode document) {
                                document.set("id", document.remove("toy_id"));
                                return document;
                            }
                        })
                        .add(new TreeMigration(SpecVersion.LATEST, toyV1) {
                            @Override
                            protected ObjectNode transform(ObjectNode document) {
                                document.set("toy_id", document.remove("id"));
                                return document;
                            }
                        })
                        .build())
                .build();
        String toyJson = "{\"schema\":\"" + SpecVersion.schemaUrl(SpecVersion.LATEST)
                + "\",\"version\":\"1\",\"toy_id\":\"toys\"}";

        GraphModel model = spec.decode(toyJson, Format.JSON, toyV1);
        String toy = spec.encode(model, Format.JSON, toyV1);
        assertEquals("toys", model.id());
        assertTrue(toy.contains("\"toy_id\""));
    }

    @Test
    void validate() throws IOException {
        GraphSpec spec = GraphSpec.defaults();
        GraphModel model = spec.decode(read("movies.json"), Format.JSON);
        assertEquals(List.of(), spec.validate(model, Validators.core()));

        Validator named = m -> m.name().isPresent()
                ? List.of()
                : List.of(Issue.of("missing_model_name", "The model has no name.", "name"));
        List<Issue> issues = spec.validate(model.toBuilder().name(null).build(), Validators.core(), named);
        assertEquals(List.of("missing_model_name"), issues.stream().map(Issue::code).collect(java.util.stream.Collectors.toList()));

        ValidatorSet mine = ValidatorSet.of(CoreValidators.REFERENCES, named);
        assertEquals(List.of(), spec.validate(model, mine));
    }
}
