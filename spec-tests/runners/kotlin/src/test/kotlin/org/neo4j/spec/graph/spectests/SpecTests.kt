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
package org.neo4j.spec.graph.spectests

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.Validations
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Reference runner for the spec tests. The cases directory and the JSON Schema are passed as the `specTests.cases`
 * and `specTests.schema` system properties. With `-PspecTests.update=true` the expected files are rewritten from this
 * implementation instead of asserted.
 */
class SpecTests {

    private val cases = File(System.getProperty("specTests.cases"))
    private val update = System.getProperty("specTests.update").toBoolean()
    private val pretty = Json { prettyPrint = true }

    private val sets = mapOf(
        "core" to Validations.core,
        "labelled" to Validations.labelled,
        "consistent_schema" to Validations.consistentSchema,
        "mappable" to Validations.mappable,
        "bulk_importable" to Validations.bulkImportable,
        "well_constrained" to Validations.wellConstrained,
        "all" to Validations.all
    )

    @TestFactory
    fun documents(): List<DynamicTest> = caseDirs("documents").map { dir ->
        dynamicTest(dir.name) {
            val json = dir.resolve("model.json")
            val yaml = dir.resolve("model.yaml")
            if (update) {
                val model = decode(documentFiles(dir).first { it.exists() })
                json.writeText(prettyJson(GraphSpec.Json.encodeToString(model)))
                yaml.writeText(GraphSpec.Yaml.encodeToString(model))
                return@dynamicTest
            }
            for (file in documentFiles(dir)) {
                val model = decode(file)
                assertJsonEquals(json.readText(), GraphSpec.Json.encodeToString(model), "${file.name} encoded as JSON")
                assertEquals(yaml.readText(), GraphSpec.Yaml.encodeToString(model), "${file.name} encoded as YAML")
            }
            assertEquals(emptyList(), decode(json).validate(Validations.core), "core issues")
        }
    }

    /** Every JSON document in a documents case is valid against the published JSON Schema. */
    @TestFactory
    fun schema(): List<DynamicTest> {
        val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(File(System.getProperty("specTests.schema")).inputStream())
        val mapper = ObjectMapper()
        return caseDirs("documents").flatMap(::documentFiles)
            .filter { it.extension == "json" }
            .map { file ->
                dynamicTest(file.relativeTo(cases).path) {
                    val errors = schema.validate(mapper.readTree(file))
                    assertTrue(errors.isEmpty(), "schema errors: $errors")
                }
            }
    }

    @TestFactory
    fun validate(): List<DynamicTest> = caseDirs("validate").flatMap { set ->
        caseDirs("validate/${set.name}")
    }.map { dir ->
        val set = dir.parentFile.name
        dynamicTest("$set/${dir.name}") {
            val issues = decode(inputFile(dir)!!).validate(sets.getValue(set))
            val expectedIssues = dir.resolve("issues.json")
            if (update) {
                val sorted = issues.map { it.toValidationIssue() }.sortedBy { it.toString() }
                expectedIssues.writeText(pretty.encodeToString(JsonElement.serializer(), JsonArray(sorted)) + "\n")
                return@dynamicTest
            }
            for ((code, message, path) in issues) {
                assertTrue(message.isNotBlank(), "issue $code has a message")
                assertNotNull(path, "issue $code has a path")
            }
            val actual = issues.map { it.toValidationIssue() }.sortedBy { it.toString() }
            val expected = Json.parseToJsonElement(expectedIssues.readText()).jsonArray.sortedBy { it.toString() }
            assertEquals(expected, actual, "issues for set $set")
        }
    }

    private fun caseDirs(path: String): List<File> =
        cases.resolve(path).listFiles { file -> file.isDirectory }!!.sortedBy { it.name }

    private fun inputFile(dir: File): File? = dir.listFiles { file -> file.name.startsWith("input.") }!!.singleOrNull()

    private fun documentFiles(dir: File): List<File> =
        listOf(dir.resolve("model.json"), dir.resolve("model.yaml")) +
            dir.listFiles { file -> file.extension in setOf("json", "yaml") && !file.name.startsWith("model.") }!!.sorted()

    private fun decode(file: File): GraphModel =
        (if (file.extension == "yaml") GraphSpec.Yaml else GraphSpec.Json).decodeFromString(file.readText())

    private fun Issue.toValidationIssue(): JsonElement = JsonObject(
        mapOf(
            "code" to JsonPrimitive(code),
            "path" to (path?.let(::JsonPrimitive) ?: JsonNull),
            "details" to JsonObject(details.mapValues { JsonPrimitive(it.value) })
        )
    )

    private fun prettyJson(json: String): String =
        pretty.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(json)) + "\n"

    private fun assertJsonEquals(expected: String, actual: String, message: String) =
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual), message)
}
