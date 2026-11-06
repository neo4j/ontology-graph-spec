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
package org.neo4j.spec.graph

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import org.neo4j.spec.graph.validate.Validations
import java.io.File
import kotlin.test.assertEquals

/**
 * Checks the library against the language-neutral corpus in /conformance, which the generated Java library is also
 * tested against. Run with -PupdateConformance to regenerate the expected files from this implementation.
 */
class ConformanceTest {
    private val root = File("conformance")
    private val update = System.getProperty("updateConformance").toBoolean()
    private val pretty = Json { prettyPrint = true }

    @TestFactory
    fun conformance(): List<DynamicTest> = Json.parseToJsonElement(File(root, "cases.json").readText()).jsonArray.map {
        val case = it.jsonObject
        val name = case.getValue("name").jsonPrimitive.content
        dynamicTest(name) {
            val input = File(case.getValue("input").jsonPrimitive.content).readText()
            val type = case.getValue("type").jsonPrimitive.content
            val outputs = outputs(input, type)
            val dir = File(root, "expected/$name")
            for ((file, actual) in outputs) {
                val expectedFile = File(dir, file)
                if (update) {
                    dir.mkdirs()
                    expectedFile.writeText(pretty.encodeToString(JsonElement.serializer(), actual) + "\n")
                } else {
                    assertEquals(Json.parseToJsonElement(expectedFile.readText()), actual, "$name/$file")
                }
            }
        }
    }

    private fun outputs(input: String, type: String): Map<String, JsonElement> {
        val model = GraphSpec.Json.decodeFromString(input, type)
        val graphSpec = Json.parseToJsonElement(GraphSpec.Json.encodeToString(model))
        val issues = JsonArray(
            model.validate(Validations.all).map { issue ->
                buildJsonObject {
                    put("code", issue.code)
                    put("message", issue.message)
                    put("path", issue.path)
                    put("details", buildJsonObject { issue.details.forEach { (k, v) -> put(k, JsonPrimitive(v)) } })
                }
            }
        )
        val dataModel = Json.parseToJsonElement(GraphSpec.Json.encodeToString(model, Type.DATA_MODEL, Version.DATA_MODEL_V3_0))
        return linkedMapOf("graph-spec.json" to graphSpec, "issues.json" to issues, "data-model.json" to dataModel)
    }
}
