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
import org.neo4j.spec.graph.model.extension.mapping.PropertyMapping
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipEndpointMappingTest {

    private val validator = RelationshipEndpointMapping
    private val relationship = Relationship(
        type = "KNOWS",
        from = RelationshipTarget(node = "node0"),
        to = RelationshipTarget(node = "node1")
    )

    private fun mapped(label: String) = TargetMapping(
        label = label,
        properties = mutableMapOf("name" to PropertyMapping("name"))
    )

    private fun mapping(fromNode: TargetMapping, toNode: TargetMapping) = RelationshipMapping(
        relationship = "rel0",
        table = "t",
        fromNode = fromNode,
        toNode = toNode
    )

    private fun modelWith(mappings: List<RelationshipMapping>) = GraphModel(
        version = "1",
        id = "test",
        extensions = GraphModelExtensions(
            dataSourceTables = mutableMapOf("t" to Table(source = "local")),
            dataSourceMappings = mutableMapOf("mapping0" to mappings.toMutableList())
        )
    )

    private fun validate(model: GraphModel): List<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateRelationship(model, "rel0", relationship, issues)
        return issues
    }

    @Test
    fun `pass when model has no data source`() {
        val model = GraphModel(version = "1", id = "test")

        assertTrue(validate(model).isEmpty(), "Expected no issues for unstructured models")
    }

    @Test
    fun `pass when both endpoints are mapped`() {
        val model = modelWith(listOf(mapping(mapped("Person"), mapped("Person"))))

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `fail when the source mapping is missing`() {
        val model = modelWith(listOf(mapping(TargetMapping(), mapped("Person"))))

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("missing_relationship_source_mapping", issues.first().code)
        assertEquals("extensions.data_source_mappings.mapping0[0].from_node", issues.first().path)
    }

    @Test
    fun `fail when the target mapping is missing`() {
        val model = modelWith(listOf(mapping(mapped("Person"), TargetMapping())))

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("missing_relationship_target_mapping", issues.first().code)
        assertEquals("extensions.data_source_mappings.mapping0[0].to_node", issues.first().path)
    }

    @Test
    fun `fail on both ends when the relationship has no mapping`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf("t" to Table(source = "local"))
            )
        )

        val issues = validate(model)

        assertEquals(2, issues.size)
        assertEquals("missing_relationship_source_mapping", issues[0].code)
        assertEquals("missing_relationship_target_mapping", issues[1].code)
        assertEquals("relationships.rel0", issues[0].path)
    }

    @Test
    fun `fail only for the incomplete mapping when several exist`() {
        val model = modelWith(
            listOf(
                mapping(mapped("Person"), mapped("Person")),
                mapping(TargetMapping(), mapped("Person"))
            )
        )

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("extensions.data_source_mappings.mapping0[1].from_node", issues.first().path)
    }
    private fun modelWithKeyedEndpoints(
        key: List<String>,
        fromCovers: List<String>,
        toCovers: List<String>
    ): GraphModel {
        val nodes = mutableMapOf<String, Node>()
        val mappings = mutableListOf<org.neo4j.spec.graph.model.extension.mapping.Mapping>()
        for (nodeId in listOf("node0", "node1")) {
            nodes[nodeId] = Node(
                labels = Labels(identifier = "Person"),
                properties = key.associateWith { Property() }.toMutableMap()
            )
            mappings.add(
                NodeMapping(
                    node = nodeId,
                    table = "t",
                    properties = key.associateWith { PropertyMapping(it) }.toMutableMap(),
                    key = key.toMutableList()
                )
            )
        }
        mappings.add(
            mapping(
                TargetMapping(
                    label = "Person",
                    properties = fromCovers.associateWith {
                        PropertyMapping(it)
                    }.toMutableMap()
                ),
                TargetMapping(
                    label = "Person",
                    properties = toCovers.associateWith {
                        PropertyMapping(it)
                    }.toMutableMap()
                )
            )
        )
        return GraphModel(
            version = "1",
            id = "test",
            nodes = nodes,
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf("t" to Table(source = "local")),
                dataSourceMappings = mutableMapOf("mapping0" to mappings)
            )
        )
    }

    @Test
    fun `pass when the endpoint mappings cover every key property`() {
        val model = modelWithKeyedEndpoints(listOf("id", "name"), listOf("id", "name"), listOf("id", "name"))

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `fail when the source mapping omits a key property`() {
        val model = modelWithKeyedEndpoints(listOf("id", "name"), listOf("id"), listOf("id", "name"))

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("missing_relationship_source_mapping", issues.first().code)
        assertTrue("name" in issues.first().message)
    }

    @Test
    fun `fail when the target mapping omits a key property`() {
        val model = modelWithKeyedEndpoints(listOf("id", "name"), listOf("id", "name"), listOf("id"))

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("missing_relationship_target_mapping", issues.first().code)
    }

    @Test
    fun `pass when the endpoint node has no key`() {
        val model = modelWithKeyedEndpoints(emptyList(), listOf("id"), listOf("id"))

        assertTrue(validate(model).isEmpty())
    }
}
