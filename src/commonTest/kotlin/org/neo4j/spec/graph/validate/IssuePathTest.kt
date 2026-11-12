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
package org.neo4j.spec.graph.validate

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.validate.mapping.MappingDependencies
import org.neo4j.spec.graph.validate.node.NodeAliasDuplicate
import org.neo4j.spec.graph.validate.node.NodeConstraints
import org.neo4j.spec.graph.validate.node.NodeIndexesExists
import org.neo4j.spec.graph.validate.node.NodeLabel
import org.neo4j.spec.graph.validate.node.NodeMappingKeyType
import org.neo4j.spec.graph.validate.relationship.RelationshipNodes
import org.neo4j.spec.graph.validate.table.TableColumnDuplicateName
import kotlin.test.Test
import kotlin.test.assertEquals

class IssuePathTest {

    @Test
    fun `joins segments as an RFC 6901 pointer`() {
        assertEquals("/nodes/person/properties/name", jsonPointerOf("nodes", "person", "properties", "name"))
    }

    @Test
    fun `array indexes are plain segments`() {
        assertEquals(
            "/extensions/data_source_mappings/m0/1/key",
            jsonPointerOf("extensions", "data_source_mappings", "m0", 1, "key")
        )
    }

    @Test
    fun `dots and brackets in keys need no escaping`() {
        assertEquals("/nodes/weird.key[0]/labels", jsonPointerOf("nodes", "weird.key[0]", "labels"))
    }

    @Test
    fun `tildes and slashes are escaped`() {
        assertEquals("/nodes/a~0b/c~1d", jsonPointerOf("nodes", "a~b", "c/d"))
    }

    @Test
    fun `a key that looks pre-escaped is escaped again`() {
        assertEquals("/a~01b", jsonPointerOf("a~1b"))
        assertEquals("/a~1b~0c~1d", jsonPointerOf("a/b~c/d"))
    }

    @Test
    fun `issue paths resolve into the source document`() {
        val json = """
        {
            "schema": "https://static.neo4j.com/graph-spec/1.0.0/schema.json",
            "version": "1",
            "id": "test",
            "nodes": {
                "n0": {
                    "labels": { "identifier": "" },
                    "aliases": ["dup", "dup"],
                    "properties": { "email": { "type": "FLOAT" } },
                    "constraints": {
                        "c0": { "type": "UNIQUE", "properties": ["missing"] }
                    },
                    "extensions": {
                        "indexes": {
                            "i0": { "type": "TEXT", "labels": ["Person"], "properties": ["missing"] }
                        }
                    }
                },
                "weird.key": {
                    "labels": { "identifier": "Thing" },
                    "properties": {},
                    "constraints": {
                        "c1": { "type": "UNIQUE", "properties": ["also"] }
                    }
                }
            },
            "relationships": {
                "r0": { "type": "KNOWS", "from": { "node": "n0" }, "to": { "node": "ghost" } }
            },
            "extensions": {
                "data_source_tables": {
                    "t0": { "source": "local", "columns": [ { "name": "a" }, { "name": "a" } ] }
                },
                "data_source_mappings": {
                    "m0": [
                        { "type": "NODE", "node": "n0", "table": "t0", "properties": {}, "key": ["email"] }
                    ]
                }
            }
        }
        """.trimIndent()
        val model = GraphSpec.Json.decodeFromString(json)
        val validators = listOf(
            NodeLabel,
            NodeAliasDuplicate,
            NodeConstraints,
            NodeIndexesExists,
            NodeMappingKeyType,
            RelationshipNodes,
            TableColumnDuplicateName,
            MappingDependencies
        )
        val issues = mutableListOf<Issue>()

        validators.forEach { it.validate(model, issues) }

        val document = Json.parseToJsonElement(json)
        val resolved = issues.associate { it.code to document.resolvePointer(it.path!!) }
        assertEquals(JsonPrimitive(""), resolved["missing_node_identifier_label"])
        assertEquals(JsonPrimitive("dup"), resolved["duplicate_node_alias"])
        assertEquals(JsonPrimitive("missing"), resolved["missing_node_index_property"])
        assertEquals(JsonPrimitive("email"), resolved["invalid_node_mapping_key_type"])
        assertEquals(JsonPrimitive("ghost"), resolved["missing_relationship_to_node"])
        assertEquals(JsonPrimitive("a"), resolved["duplicate_table_column_name"])
        val constraintIssues = issues.filter { it.code == "missing_node_constraint_property" }
        assertEquals(
            JsonPrimitive("missing"),
            document.resolvePointer(constraintIssues.first { it.path!!.startsWith("/nodes/n0") }.path!!)
        )
        assertEquals(
            JsonPrimitive("also"),
            document.resolvePointer(constraintIssues.first { it.path!!.startsWith("/nodes/weird.key") }.path!!)
        )
    }

    private fun JsonElement.resolvePointer(pointer: String): JsonElement? {
        var current: JsonElement = this
        for (rawSegment in pointer.split('/').drop(1)) {
            val segment = rawSegment.replace("~1", "/").replace("~0", "~")
            current = when (current) {
                is JsonObject -> current[segment] ?: return null
                is JsonArray -> segment.toIntOrNull()?.let { current.getOrNull(it) } ?: return null
                else -> return null
            }
        }
        return current
    }
}
