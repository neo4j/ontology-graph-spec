package org.neo4j.spec.graph.spectests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.spec.graph.Format;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.ValidatorSet;
import org.neo4j.spec.graph.validate.Validators;

/** Runs the Java library against the language-neutral cases under spec-tests/cases. */
class SpecTests {

    private static final Path CASES = Paths.get(System.getProperty("specTests.cases", "../../cases"));
    private static final Path JAVA_CASES = Paths.get(System.getProperty("specTests.javaCases", "src/test/resources/cases"));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final GraphSpec SPEC = GraphSpec.defaults();

    private static final Map<String, java.util.function.Supplier<ValidatorSet>> SETS =
            Map.of(
                    "core", Validators::core,
                    "labelled", Validators::labelled,
                    "consistent_schema", Validators::consistentSchema,
                    "well_constrained", Validators::wellConstrained,
                    "all", Validators::all);

    @TestFactory
    Stream<DynamicTest> documents() {
        return caseDirs(CASES.resolve("documents"))
                .map(dir -> DynamicTest.dynamicTest("documents/" + dir.getFileName(), () -> document(dir)));
    }

    @TestFactory
    Stream<DynamicTest> validate() {
        return Stream.concat(
                validationTests(CASES.resolve("validate"), "validate/"),
                validationTests(JAVA_CASES.resolve("validate"), "java/validate/"));
    }

    private static Stream<DynamicTest> validationTests(Path root, String prefix) {
        return caseDirs(root).flatMap(set -> caseDirs(set)
                .map(dir -> DynamicTest.dynamicTest(
                        prefix + set.getFileName() + "/" + dir.getFileName(),
                        () -> validation(set.getFileName().toString(), dir))));
    }

    private static void document(Path dir) {
        JsonNode expectedJson = parseJson(read(dir.resolve("model.json")));
        String expectedYaml = read(dir.resolve("model.yaml"));
        for (Path file : documentFiles(dir)) {
            GraphModel model = decode(file);
            String name = file.getFileName().toString();
            assertEquals(expectedJson, parseJson(SPEC.encode(model, Format.JSON)), name + " encoded as JSON");
            assertEquals(expectedYaml, SPEC.encode(model, Format.YAML), name + " encoded as YAML");
        }
        assertEquals(List.of(), SPEC.validate(decode(dir.resolve("model.json")), Validators.core()), "core issues");
    }

    private static void validation(String set, Path dir) {
        if (!SETS.containsKey(set)) {
            fail("Unknown validator set: " + set);
        }
        List<JsonNode> expected = sorted(parseJson(read(dir.resolve("issues.json"))));
        for (String input : List.of("input.json", "input.yaml")) {
            List<Issue> issues = SPEC.validate(decode(dir.resolve(input)), SETS.get(set).get());
            List<JsonNode> actual = new ArrayList<>();
            for (Issue issue : issues) {
                assertFalse(issue.message().isBlank(), "issue " + issue.code() + " has a message");
                assertTrue(issue.path().isPresent(), "issue " + issue.code() + " has a path");
                actual.add(toJson(issue));
            }
            assertEquals(expected, sorted(actual), "issues for " + input + " with set " + set);
        }
    }

    private static JsonNode toJson(Issue issue) {
        ObjectNode node = JSON.createObjectNode();
        node.put("code", issue.code());
        node.put("path", issue.path().orElse(null));
        ObjectNode details = node.putObject("details");
        issue.details().forEach(details::put);
        return node;
    }

    private static List<JsonNode> sorted(Iterable<JsonNode> issues) {
        List<JsonNode> list = new ArrayList<>();
        issues.forEach(list::add);
        list.sort(Comparator.comparing(JsonNode::toString));
        return list;
    }

    private static GraphModel decode(Path file) {
        Format format = file.toString().endsWith(".yaml") ? Format.YAML : Format.JSON;
        return SPEC.decode(read(file), format);
    }

    private static List<Path> documentFiles(Path dir) {
        List<Path> files = new ArrayList<>(List.of(dir.resolve("model.json"), dir.resolve("model.yaml")));
        try (Stream<Path> others = Files.list(dir)) {
            files.addAll(others.filter(path -> {
                        String name = path.getFileName().toString();
                        return (name.endsWith(".json") || name.endsWith(".yaml")) && !name.startsWith("model.");
                    })
                    .sorted()
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    private static Stream<Path> caseDirs(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList()).stream();
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

    private static JsonNode parseJson(String text) {
        try {
            return JSON.readTree(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
