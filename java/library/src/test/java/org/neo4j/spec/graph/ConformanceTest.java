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
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.spec.graph.codec.format.JsonFormat;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validations;

/** Runs the generated library against the corpus in /conformance, whose expected files come from the Kotlin library. */
class ConformanceTest {
    private static final Path ROOT = Path.of(System.getProperty("conformanceRoot", "../.."));
    private static final ObjectMapper MAPPER = JsonFormat.builder().build();

    @TestFactory
    Stream<DynamicTest> conformance() throws IOException {
        JsonNode cases = MAPPER.readTree(ROOT.resolve("conformance/cases.json").toFile());
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode testCase : cases) {
            String name = testCase.get("name").asText();
            tests.add(dynamicTest(name, () -> {
                String input = Files.readString(ROOT.resolve(testCase.get("input").asText()));
                Map<String, JsonNode> outputs = outputs(input, testCase.get("type").asText());
                for (Map.Entry<String, JsonNode> output : outputs.entrySet()) {
                    Path expected = ROOT.resolve("conformance/expected").resolve(name).resolve(output.getKey());
                    assertEquals(MAPPER.readTree(expected.toFile()), output.getValue(), name + "/" + output.getKey());
                }
            }));
        }
        return tests.stream();
    }

    private static Map<String, JsonNode> outputs(String input, String type) throws IOException {
        GraphModel model = GraphSpec.getJson().decodeFromString(input, type);
        Map<String, JsonNode> outputs = new LinkedHashMap<>();
        outputs.put("graph-spec.json", MAPPER.readTree(GraphSpec.getJson().encodeToString(model)));
        ArrayNode issues = MAPPER.createArrayNode();
        for (Issue issue : model.validate(Validations.getAll())) {
            ObjectNode node = issues.addObject();
            node.put("code", issue.getCode());
            node.put("message", issue.getMessage());
            node.put("path", issue.getPath());
            ObjectNode details = node.putObject("details");
            issue.getDetails().forEach(details::put);
        }
        outputs.put("issues.json", issues);
        outputs.put("data-model.json", MAPPER.readTree(GraphSpec.getJson().encodeToString(model, Type.DATA_MODEL, Version.DATA_MODEL_V3_0)));
        return outputs;
    }
}
