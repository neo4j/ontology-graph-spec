package org.neo4j.spec.graph.spectests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.spec.graph.internal.yaml.YamlEmitter;

/** Checks the YAML emitter reproduces every documents case's model.yaml exactly. */
class YamlEmitterTests {

    private static final Path CASES = Paths.get(System.getProperty("specTests.cases", "../../cases"));
    private static final ObjectMapper JSON = new ObjectMapper();

    @TestFactory
    Stream<DynamicTest> documents() throws IOException {
        List<Path> dirs;
        try (Stream<Path> list = Files.list(CASES.resolve("documents"))) {
            dirs = list.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        }
        return dirs.stream().map(dir -> DynamicTest.dynamicTest("documents/" + dir.getFileName(), () -> {
            JsonNode json = JSON.readTree(read(dir.resolve("model.json")));
            assertEquals(read(dir.resolve("model.yaml")), YamlEmitter.emit(json));
        }));
    }

    private static String emit(String json) {
        try {
            return YamlEmitter.emit(JSON.readTree(json));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void scalars() {
        assertEquals(
                "a: \"x\"\nb: 40\nc: -120.5\nd: 0\ne: null\nf: true\ng: 12\n",
                emit("{\"a\":\"x\",\"b\":40.0,\"c\":-120.5,\"d\":0.0,\"e\":null,\"f\":true,\"g\":12}"));
    }

    @Test
    void stringEscapes() {
        assertEquals(
                "a: \"q\\\"\\\\ \\n\\t\\r \\u0001 \\u2028 é 🎬\"\n",
                emit("{\"a\":\"q\\\"\\\\ \\n\\t\\r \\u0001 \\u2028 é 🎬\"}"));
    }

    @Test
    void keyQuoting() {
        assertEquals(
                "\"\": 1\n\"1\": 1\n\"true\": 1\nyes: 1\nwith space: 1\n\"a-b\": 1\n\"a:b\": 1\n\"#a\": 1\na.b: 1\n",
                emit("{\"\":1,\"1\":1,\"true\":1,\"yes\":1,\"with space\":1,\"a-b\":1,\"a:b\":1,\"#a\":1,\"a.b\":1}"));
    }

    @Test
    void emptiesAndNesting() {
        assertEquals(
                "a:\n  {}\nb:\n  []\nc:\n  - []\n  - {}\n  - - 1\n    - 2\n  - k: 1\n    l:\n      - x: 1\n",
                emit("{\"a\":{},\"b\":[],\"c\":[[],{},[1,2],{\"k\":1,\"l\":[{\"x\":1}]}]}"));
    }

    private static String read(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
