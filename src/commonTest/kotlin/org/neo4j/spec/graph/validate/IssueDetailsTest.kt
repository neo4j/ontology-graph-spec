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

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.extension.source.TableColumn
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.node.extension.NodeExtensions
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.length.StringLength
import org.neo4j.spec.graph.validate.mapping.MappingDependencies
import org.neo4j.spec.graph.validate.node.NodeIndexProperties
import org.neo4j.spec.graph.validate.node.NodeLabel
import org.neo4j.spec.graph.validate.node.NodePropertyEmptyName
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintProperties
import org.neo4j.spec.graph.validate.relationship.RelationshipType
import org.neo4j.spec.graph.validate.table.TableColumnType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IssueDetailsTest {

    private fun validate(validator: Validation, model: GraphModel): List<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validate(model, issues)
        return issues
    }

    private fun assertAnchor(issues: List<Issue>, keyType: String, key: String) {
        assertTrue(issues.isNotEmpty(), "Expected at least one issue")
        assertEquals(keyType, issues.first().details["keyType"])
        assertEquals(key, issues.first().details["key"])
    }

    @Test
    fun `key types serialize as lowercase names`() {
        assertEquals(
            listOf("model", "node", "relationship", "property", "index", "constraint", "table", "column", "mapping"),
            IssueKeyType.entries.map { issueDetails(it, "k")["keyType"] }
        )
    }

    @Test
    fun `node issue carries the node key`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf("person" to Node(labels = Labels(identifier = "")))
        )

        assertAnchor(validate(NodeLabel, model), "node", "person")
    }

    @Test
    fun `relationship issue carries the relationship key`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            relationships = mutableMapOf(
                "rel0" to Relationship(
                    type = "",
                    from = RelationshipTarget(node = "a"),
                    to = RelationshipTarget(node = "b")
                )
            )
        )

        assertAnchor(validate(RelationshipType, model), "relationship", "rel0")
    }

    @Test
    fun `property issue carries the property key`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf(
                "person" to Node(
                    labels = Labels(identifier = "Person"),
                    properties = mutableMapOf("" to Property())
                )
            )
        )

        val issues = validate(NodePropertyEmptyName, model)
        assertTrue(issues.isNotEmpty())
        assertEquals("property", issues.first().details["keyType"])
        assertEquals("", issues.first().details["key"])
    }

    @Test
    fun `index issue carries the index key`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf(
                "person" to Node(
                    labels = Labels(identifier = "Person"),
                    extensions = NodeExtensions(
                        indexes = mutableMapOf(
                            "idx0" to NodeIndex(
                                type = IndexType.RANGE,
                                labels = mutableListOf("Person"),
                                properties = mutableListOf()
                            )
                        )
                    )
                )
            )
        )

        assertAnchor(validate(NodeIndexProperties, model), "index", "idx0")
    }

    @Test
    fun `constraint issue carries the constraint key`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf(
                "person" to Node(
                    labels = Labels(identifier = "Person"),
                    constraints = mutableMapOf(
                        "c0" to NodeConstraint(type = ConstraintType.UNIQUE, properties = mutableListOf())
                    )
                )
            )
        )

        assertAnchor(validate(NodeConstraintProperties, model), "constraint", "c0")
    }

    @Test
    fun `column issue carries the column name`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf(
                    "t" to Table(source = "cloud", columns = mutableListOf(TableColumn(name = "a", type = "x")))
                )
            )
        )

        assertAnchor(validate(TableColumnType, model), "column", "a")
    }

    @Test
    fun `mapping issue carries the mapping key`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceMappings = mutableMapOf(
                    "m0" to mutableListOf(
                        NodeMapping(node = "person", table = "t", properties = mutableMapOf(), id = "dup"),
                        NodeMapping(node = "other", table = "t", properties = mutableMapOf(), id = "dup")
                    )
                )
            )
        )

        assertAnchor(validate(MappingDependencies, model), "mapping", "dup")
    }

    @Test
    fun `model issue carries the model key`() {
        val model = GraphModel(version = "1", id = "test", name = "x".repeat(300))

        assertAnchor(validate(StringLength, model), "model", "test")
    }

    @Test
    fun `every issue from every built-in validator carries an anchor`() {
        val broken = GraphModel(
            version = "1",
            id = "test",
            name = "x".repeat(300),
            nodes = mutableMapOf(
                "person" to Node(
                    labels = Labels(identifier = ""),
                    aliases = mutableListOf("dup", "dup"),
                    properties = mutableMapOf("" to Property()),
                    constraints = mutableMapOf(
                        "c0" to NodeConstraint(type = ConstraintType.UNIQUE, properties = mutableListOf("ghost"))
                    ),
                    extensions = NodeExtensions(
                        indexes = mutableMapOf(
                            "idx0" to NodeIndex(
                                type = IndexType.RANGE,
                                labels = mutableListOf("Person", "Person"),
                                properties = mutableListOf("ghost")
                            )
                        )
                    )
                ),
                "other" to Node(labels = Labels(identifier = "Other"))
            ),
            relationships = mutableMapOf(
                "rel0" to Relationship(
                    type = "",
                    from = RelationshipTarget(node = "ghost"),
                    to = RelationshipTarget(node = "other")
                )
            ),
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf(
                    "t" to Table(
                        source = "cloud",
                        columns = mutableListOf(TableColumn(name = "", type = "x"))
                    )
                ),
                dataSourceMappings = mutableMapOf(
                    "m0" to mutableListOf(
                        NodeMapping(node = "other", table = "t", properties = mutableMapOf()),
                        RelationshipMapping(
                            relationship = "rel0",
                            table = "t",
                            fromNode = TargetMapping(),
                            toNode = TargetMapping()
                        )
                    )
                )
            )
        )
        val issues = mutableListOf<Issue>()

        Validations.all.forEach { it.validate(broken, issues) }

        assertTrue(issues.isNotEmpty(), "Expected the broken model to produce issues")
        val untyped = issues.filter { it.details["keyType"].isNullOrEmpty() }
        assertTrue(
            untyped.isEmpty(),
            "Issues without a keyType: ${untyped.map { it.code }}"
        )
    }
}
