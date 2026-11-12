package org.neo4j.spec.graph.spectests;

import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.Validations;

/**
 * Runs the Java API against the spec tests. The cases directory is passed as the {@code specTests.cases} system
 * property.
 */
class SpecTests {

    private static final Path CASES = Path.of(System.getProperty("specTests.cases"));
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Map<String, List<Validation>> SETS = Map.of(
        "core", Validations.getCore(),
        "labelled", Validations.getLabelled(),
        "consistent_schema", Validations.getConsistentSchema(),
        "mappable", Validations.getMappable(),
        "bulk_importable", Validations.getBulkImportable(),
        "well_constrained", Validations.getWellConstrained(),
        "all", Validations.getAll());

    @TestFactory
    Stream<DynamicTest> documents() {
        return caseDirs("documents").map(dir -> dynamicTest(dir.getFileName().toString(), () -> {
            Path json = dir.resolve("model.json");
            Path yaml = dir.resolve("model.yaml");
            for (Path file : documentFiles(dir)) {
                GraphModel model = decode(file);
                assertEquals(
                    JSON.readTree(read(json)),
                    JSON.readTree(GraphSpec.getJson().encodeToString(model)),
                    file.getFileName() + " encoded as JSON");
                assertEquals(read(yaml), GraphSpec.getYaml().encodeToString(model), file.getFileName() + " encoded as YAML");
            }
            assertEquals(List.of(), decode(json).validate(Validations.getCore()), "core issues");
        }));
    }

    @TestFactory
    Stream<DynamicTest> validate() {
        return caseDirs("validate")
            .flatMap(set -> caseDirs("validate/" + set.getFileName()))
            .map(dir -> {
                String set = dir.getParent().getFileName().toString();
                String name = set + "/" + dir.getFileName();
                return dynamicTest(name, () -> {
                    List<Issue> issues = decode(inputFile(dir).orElseThrow()).validate(SETS.get(set));
                    for (Issue issue : issues) {
                        assertFalse(issue.getMessage().isBlank(), "issue " + issue.getCode() + " has a message");
                        assertNotNull(issue.getPath(), "issue " + issue.getCode() + " has a path");
                    }

                    List<JsonNode> actual = issues.stream().map(SpecTests::validationIssue).sorted(comparing(JsonNode::toString)).toList();
                    JsonNode expectedIssues = JSON.readTree(read(dir.resolve("issues.json")));
                    List<JsonNode> expected = StreamSupport.stream(expectedIssues.spliterator(), false)
                        .sorted(comparing(JsonNode::toString))
                        .toList();

                    assertEquals(expected, actual, "issues for set " + set);
                });
            });
    }

    private static Stream<Path> caseDirs(String path) {
        try (Stream<Path> entries = Files.list(CASES.resolve(path))) {
            return entries.filter(Files::isDirectory).sorted().toList().stream();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The canonical {@code model.json} and {@code model.yaml} first, then every alternative form of the same model. */
    private static List<Path> documentFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>(List.of(dir.resolve("model.json"), dir.resolve("model.yaml")));
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(f -> f.toString().matches(".*\\.(json|yaml)$") && !f.getFileName().toString().startsWith("model."))
                .sorted()
                .forEach(files::add);
        }
        return files;
    }

    private static Optional<Path> inputFile(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.getFileName().toString().startsWith("input.")).findFirst();
        }
    }

    private static GraphModel decode(Path file) throws IOException {
        GraphSpec spec = file.toString().endsWith(".yaml") ? GraphSpec.getYaml() : GraphSpec.getJson();
        return spec.decodeFromString(read(file));
    }

    private static JsonNode validationIssue(Issue issue) {
        ObjectNode node = JSON.createObjectNode().put("code", issue.getCode()).put("path", issue.getPath());
        node.set("details", JSON.valueToTree(issue.getDetails()));
        return node;
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file);
    }
}
