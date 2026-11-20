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
package org.neo4j.spec.graph.validate.node

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.node.extension.NodeExtensions
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeIndexConstraintNameConflictTest {

    private val validator = NodeIndexConstraintNameConflict

    private fun index(
        name: String?,
        properties: List<String>,
        label: String = "Person",
        type: IndexType = IndexType.RANGE
    ) = NodeIndex(
        type = type,
        labels = mutableListOf(label),
        properties = properties.toMutableList(),
        name = name
    )

    private fun constraint(name: String?, properties: List<String>, type: ConstraintType = ConstraintType.UNIQUE) =
        NodeConstraint(
            type = type,
            properties = properties.toMutableList(),
            name = name
        )

    private fun node(
        indexes: Map<String, NodeIndex> = emptyMap(),
        constraints: Map<String, NodeConstraint> = emptyMap(),
        label: String = "Person"
    ) = Node(
        labels = Labels(identifier = label),
        properties = mutableMapOf("id" to Property(), "name" to Property()),
        extensions = NodeExtensions(indexes = indexes.toMutableMap()),
        constraints = constraints.toMutableMap()
    )

    private fun validate(vararg nodes: Pair<String, Node>): List<Issue> {
        val model = GraphModel(version = "1", id = "test")
        model.nodes.putAll(nodes.toMap())
        val issues = mutableListOf<Issue>()
        validator.validate(model, issues)
        return issues
    }

    @Test
    fun `fail when indexes sharing a name have identical definitions`() {
        // Graph-spec does not carry UPX's identical-definition exception; any duplicate name is flagged.
        // ARRANGE
        val node = node(
            indexes = mapOf(
                "i1" to index("dup", listOf("id")),
                "i2" to index("dup", listOf("id"))
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(2, issues.size)
        assertTrue(issues.all { it.code == "duplicate_node_index_constraint_name" })
        assertEquals(
            setOf("/nodes/personNode/extensions/indexes/i1/name", "/nodes/personNode/extensions/indexes/i2/name"),
            issues.mapNotNull { it.path }.toSet()
        )
    }

    @Test
    fun `fail when indexes sharing a name differ only in property order`() {
        // ARRANGE
        val node = node(
            indexes = mapOf(
                "i1" to index("dup", listOf("id", "name")),
                "i2" to index("dup", listOf("name", "id"))
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(2, issues.size)
    }

    @Test
    fun `fail when indexes sharing a name have different index types`() {
        // UPX probe case C
        // ARRANGE
        val node = node(
            indexes = mapOf(
                "i1" to index("dup", listOf("id"), type = IndexType.RANGE),
                "i2" to index("dup", listOf("id"), type = IndexType.TEXT)
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(2, issues.size, "Every index sharing the bad name is flagged")
        assertTrue(issues.all { it.code == "duplicate_node_index_constraint_name" })
        assertEquals(
            setOf("/nodes/personNode/extensions/indexes/i1/name", "/nodes/personNode/extensions/indexes/i2/name"),
            issues.mapNotNull { it.path }.toSet()
        )
    }

    @Test
    fun `fail when three indexes share a name`() {
        // ARRANGE
        val node = node(
            indexes = mapOf(
                "i1" to index("dup", listOf("id")),
                "i2" to index("dup", listOf("id")),
                "i3" to index("dup", listOf("name"))
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(3, issues.size)
    }

    @Test
    fun `fail when indexes on different nodes share a name`() {
        // Name scope is the whole model.
        // ARRANGE
        val personNode = node(indexes = mapOf("i1" to index("dup", listOf("id"), label = "Person")))
        val movieNode = node(
            indexes = mapOf("i2" to index("dup", listOf("id"), label = "Movie")),
            label = "Movie"
        )

        // ACT
        val issues = validate("personNode" to personNode, "movieNode" to movieNode)

        // ASSERT
        assertEquals(2, issues.size)
        assertEquals(
            setOf("/nodes/personNode/extensions/indexes/i1/name", "/nodes/movieNode/extensions/indexes/i2/name"),
            issues.mapNotNull { it.path }.toSet()
        )
    }

    @Test
    fun `fail when constraints sharing a name have identical definitions`() {
        // ARRANGE
        val node = node(
            constraints = mapOf(
                "c1" to constraint("dup", listOf("id")),
                "c2" to constraint("dup", listOf("id"))
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(2, issues.size)
        assertEquals(
            setOf("/nodes/personNode/constraints/c1/name", "/nodes/personNode/constraints/c2/name"),
            issues.mapNotNull { it.path }.toSet()
        )
    }

    @Test
    fun `fail when constraints sharing a name have different constraint types`() {
        // UPX probe case G
        // ARRANGE
        val node = node(
            constraints = mapOf(
                "c1" to constraint("dup", listOf("id"), type = ConstraintType.UNIQUE),
                "c2" to constraint("dup", listOf("id"), type = ConstraintType.KEY)
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(2, issues.size)
        assertEquals(
            setOf("/nodes/personNode/constraints/c1/name", "/nodes/personNode/constraints/c2/name"),
            issues.mapNotNull { it.path }.toSet()
        )
    }

    @Test
    fun `fail when an index and a constraint share a name`() {
        // UPX probe case H: always an error, even with the same properties and label
        // ARRANGE
        val node = node(
            indexes = mapOf("i1" to index("dup", listOf("id"))),
            constraints = mapOf("c1" to constraint("dup", listOf("id")))
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(2, issues.size)
        assertEquals(
            setOf("/nodes/personNode/extensions/indexes/i1/name", "/nodes/personNode/constraints/c1/name"),
            issues.mapNotNull { it.path }.toSet()
        )
    }

    @Test
    fun `fail when an index shares a name with two identical constraints`() {
        // UPX probe case I: all three are flagged
        // ARRANGE
        val node = node(
            indexes = mapOf("i1" to index("dup", listOf("id"))),
            constraints = mapOf(
                "c1" to constraint("dup", listOf("id")),
                "c2" to constraint("dup", listOf("id"))
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertEquals(3, issues.size)
    }

    @Test
    fun `pass when names are missing or blank`() {
        // UPX probe case J: falsy names are skipped
        // ARRANGE
        val node = node(
            indexes = mapOf(
                "i1" to index(null, listOf("id")),
                "i2" to index(null, listOf("name")),
                "i3" to index("", listOf("id")),
                "i4" to index("", listOf("name"))
            )
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertTrue(issues.isEmpty(), "Unnamed indexes are not name conflicts")
    }

    @Test
    fun `pass when names are distinct`() {
        // UPX probe case K
        // ARRANGE
        val node = node(
            indexes = mapOf(
                "i1" to index("a", listOf("id")),
                "i2" to index("b", listOf("name"))
            ),
            constraints = mapOf("c1" to constraint("c", listOf("id")))
        )

        // ACT
        val issues = validate("personNode" to node)

        // ASSERT
        assertTrue(issues.isEmpty())
    }
}
