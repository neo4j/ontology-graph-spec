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
package org.neo4j.spec.graph.bridge

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {

    private val input = """{
            "schema": "https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json",
            "version": "1",
            "id": "test",
            "nodes": {
                "n": {
                    "constraints": {
                        "c1": {
                            "type": "EXISTS",
                            "properties": [],
                            "name": "constraint1"
                        }
                    }
                }
            }
        }
        """.trimIndent()

    @Test
    fun testValidate() {
        val result = validateContent(input, "all")
        listOf(
            "invalid_node_exist_constraint_property_count",
            "missing_node_identifier_label",
            "missing_node_properties",
            "missing_node_constraint_properties"
        ).forEach { code ->
            assertTrue(result.contains("\"code\":\"$code\""), "Expected issue $code in $result")
        }
    }

    @Test
    fun testValidateMultipleSets() {
        val result = validateContent(input, "labelled,well_constrained")
        listOf("missing_node_identifier_label", "missing_node_properties").forEach { code ->
            assertTrue(result.contains("\"code\":\"$code\""), "Expected issue $code in $result")
        }
        listOf("invalid_node_exist_constraint_property_count", "missing_node_constraint_properties").forEach { code ->
            assertFalse(result.contains("\"code\":\"$code\""), "Unexpected issue $code in $result")
        }
    }

    @Test
    fun testValidateOverlappingSetsReportsIssuesOnce() {
        assertEquals(validateContent(input, "all,core,labelled,all"), validateContent(input, "all"))
    }

    @Test
    fun testValidateUnknownSetAmongMany() {
        val error = assertFailsWith<IllegalArgumentException> {
            validateContent(input, "core,unknown")
        }
        assertContains(error.message!!, "unknown validation type: unknown")
    }

    @Test
    fun testValidateEverySet() {
        val validationSets = listOf(
            "core", "labelled", "consistent_schema", "mappable", "well_constrained", "all"
        )
        for (validation in validationSets) {
            val result = validateContent(input, validation)
            assertTrue(result.isNotBlank() && result.isNotEmpty())
        }

        val unknownValidationSet = "unknown"
        val error = assertFailsWith<IllegalArgumentException> {
            validateContent(input, unknownValidationSet)
        }
        assertContains(error.message!!, "unknown validation type")
    }
}
