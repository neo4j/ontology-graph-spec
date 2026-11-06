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
package org.neo4j.spec.graph.validate.relationship

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipMappingKeyTest {

    private fun model(key: List<String>, relationship: String = "rel0") = GraphModel(
        version = "1",
        id = "test",
        nodes = mutableMapOf("node0" to Node(properties = mutableMapOf("shared" to Property(), "age" to Property()))),
        relationships = mutableMapOf(
            "rel0" to Relationship(
                type = "KNOWS",
                from = RelationshipTarget(node = "node0"),
                to = RelationshipTarget(node = "node0"),
                properties = mutableMapOf("since" to Property(), "shared" to Property())
            )
        ),
        extensions = GraphModelExtensions(
            dataSourceMappings = mutableMapOf(
                "m0" to mutableListOf(
                    NodeMapping(node = "node0", table = "t", properties = mutableMapOf(), key = mutableListOf("age")),
                    RelationshipMapping(
                        relationship = relationship,
                        table = "t",
                        fromNode = TargetMapping(),
                        toNode = TargetMapping(),
                        key = key.toMutableList()
                    )
                )
            )
        )
    )

    private fun validate(model: GraphModel) = mutableListOf<Issue>().also { RelationshipMappingKey.validate(model, it) }

    @Test
    fun `pass when key only contains relationship properties`() {
        assertTrue(validate(model(listOf("since", "shared"))).isEmpty())
    }

    @Test
    fun `pass when there is no key`() {
        assertTrue(validate(model(emptyList())).isEmpty())
    }

    @Test
    fun `fail when key contains a property of a node at either end`() {
        val issues = validate(model(listOf("since", "age")))

        assertEquals(1, issues.size)
        assertEquals("invalid_relationship_mapping_key", issues.first().code)
        assertEquals("extensions.data_source_mappings.m0[1].key", issues.first().path)
        assertEquals("age", issues.first().details["property"])
    }

    @Test
    fun `fail when key contains an unknown property`() {
        val issues = validate(model(listOf("missing")))

        assertEquals(listOf("missing"), issues.map { it.details["property"] })
    }

    @Test
    fun `ignore mappings of other relationships`() {
        assertTrue(validate(model(listOf("age"), relationship = "other")).isEmpty())
    }
}
