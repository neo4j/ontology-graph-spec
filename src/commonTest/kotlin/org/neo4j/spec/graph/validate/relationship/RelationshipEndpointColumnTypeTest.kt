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
import org.neo4j.spec.graph.model.extension.source.TableColumn
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipEndpointColumnTypeTest {

    private val validator = RelationshipEndpointColumnType

    private fun cloudColumn(name: String, vararg supported: Neo4jType) =
        TableColumn(name = name, type = "varchar", supported = supported.toSet())

    private fun localColumn(name: String) = TableColumn(name = name, type = "varchar")

    private fun model(
        nodeColumn: TableColumn,
        relSourceColumn: TableColumn?,
        relTargetColumn: TableColumn?,
        nodePropertyMapped: Boolean = true
    ): GraphModel {
        val relColumns = listOfNotNull(relSourceColumn, relTargetColumn).toMutableList()
        return GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf(
                "node0" to Node(
                    labels = Labels(identifier = "Person"),
                    properties = mutableMapOf("name" to Property(type = Neo4jType.STRING))
                )
            ),
            relationships = mutableMapOf(
                "rel0" to Relationship(
                    type = "KNOWS",
                    from = RelationshipTarget(node = "node0"),
                    to = RelationshipTarget(node = "node0")
                )
            ),
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf(
                    "nodes" to Table(source = "cloud", columns = mutableListOf(nodeColumn)),
                    "rels" to Table(source = "cloud", columns = relColumns)
                ),
                dataSourceMappings = mutableMapOf(
                    "mapping0" to mutableListOf(
                        NodeMapping(
                            node = "node0",
                            table = "nodes",
                            properties =
                            if (nodePropertyMapped) {
                                mutableMapOf("name" to PropertyMapping(nodeColumn.name))
                            } else {
                                mutableMapOf()
                            },
                            key = mutableSetOf("name")
                        ),
                        RelationshipMapping(
                            relationship = "rel0",
                            table = "rels",
                            fromNode = TargetMapping(
                                label = "Person",
                                properties = relSourceColumn?.let {
                                    mutableMapOf("name" to PropertyMapping(it.name))
                                } ?: mutableMapOf()
                            ),
                            toNode = TargetMapping(
                                label = "Person",
                                properties = relTargetColumn?.let {
                                    mutableMapOf("name" to PropertyMapping(it.name))
                                } ?: mutableMapOf()
                            )
                        )
                    )
                )
            )
        )
    }

    private fun validate(model: GraphModel): List<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateRelationship(model, "rel0", model.relationships.getValue("rel0"), issues)
        return issues
    }

    @Test
    fun `pass when the endpoint column supports the key property type`() {
        val model = model(
            cloudColumn("name", Neo4jType.STRING),
            cloudColumn("src", Neo4jType.STRING, Neo4jType.INTEGER),
            cloudColumn("dst", Neo4jType.STRING)
        )

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `pass when the relationship column has no supported types`() {
        val model = model(
            cloudColumn("name", Neo4jType.STRING),
            localColumn("src"),
            localColumn("dst")
        )

        assertTrue(validate(model).isEmpty(), "Expected no issues when the relationship field is local")
    }

    @Test
    fun `pass when the node column has no supported types`() {
        val model = model(
            localColumn("name"),
            cloudColumn("src", Neo4jType.INTEGER),
            cloudColumn("dst", Neo4jType.INTEGER)
        )

        assertTrue(validate(model).isEmpty(), "Expected no issues when the node field is local")
    }

    @Test
    fun `pass when the key property is not mapped in the node mapping`() {
        val model = model(
            cloudColumn("name", Neo4jType.STRING),
            cloudColumn("src", Neo4jType.INTEGER),
            cloudColumn("dst", Neo4jType.STRING),
            nodePropertyMapped = false
        )

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `pass when the endpoint column is unknown`() {
        val model = model(
            cloudColumn("name", Neo4jType.STRING),
            null,
            cloudColumn("dst", Neo4jType.STRING)
        )

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `fail when the source column cannot convert to the key property type`() {
        val model = model(
            cloudColumn("name", Neo4jType.STRING),
            cloudColumn("src", Neo4jType.INTEGER),
            cloudColumn("dst", Neo4jType.STRING)
        )

        val issues = validate(model)

        // Replaced legacy code: NO_SUPPORTED_TYPES
        assertEquals(1, issues.size)
        assertEquals("incompatible_relationship_endpoint_column_type", issues.first().code)
        assertEquals("/extensions/data_source_mappings/mapping0/1/from_node/properties/name", issues.first().path)
        assertTrue("src" in issues.first().message && "rels" in issues.first().message)
    }

    @Test
    fun `fail when the target column cannot convert to the key property type`() {
        val model = model(
            cloudColumn("name", Neo4jType.STRING),
            cloudColumn("src", Neo4jType.STRING),
            cloudColumn("dst", Neo4jType.BOOLEAN)
        )

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("/extensions/data_source_mappings/mapping0/1/to_node/properties/name", issues.first().path)
    }
}
