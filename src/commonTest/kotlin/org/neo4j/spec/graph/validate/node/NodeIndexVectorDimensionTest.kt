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
import org.neo4j.spec.graph.model.index.IndexOption
import org.neo4j.spec.graph.model.index.VectorIndexOption
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeIndexVectorDimensionTest {

    private val validator = NodeIndexVectorDimension
    private val model = GraphModel(version = "1", id = "test")

    private fun node(property: Property) = Node(
        labels = Labels(identifier = "Person"),
        properties = mutableMapOf("embedding" to property)
    )

    private fun validate(node: Node, index: NodeIndex): MutableList<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, "person", node, "idx0", index, issues)
        return issues
    }

    private fun vectorIndex(options: IndexOption?, properties: List<String> = listOf("embedding")) = NodeIndex(
        type = IndexType.VECTOR,
        labels = mutableListOf("Person"),
        properties = properties.toMutableList(),
        options = options
    )

    @Test
    fun `pass when index is not a vector index`() {
        val index = NodeIndex(
            type = IndexType.RANGE,
            labels = mutableListOf("Person"),
            properties = mutableListOf("embedding")
        )

        assertTrue(validate(node(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)), index).isEmpty())
    }

    @Test
    fun `pass when vector index covers several properties`() {
        val index = vectorIndex(null, listOf("embedding", "other"))

        assertTrue(validate(node(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)), index).isEmpty())
    }

    @Test
    fun `pass when the indexed property is not a vector`() {
        val index = vectorIndex(null)

        assertTrue(validate(node(Property(type = Neo4jType.STRING)), index).isEmpty())
    }

    @Test
    fun `pass when the vector property has no dimension`() {
        val index = vectorIndex(null)

        assertTrue(
            validate(node(Property(type = Neo4jType.VECTOR_FLOAT)), index).isEmpty(),
            "Expected no issues; a missing property dimension is reported by the property validators"
        )
    }

    @Test
    fun `pass when index and property dimensions match`() {
        val index = vectorIndex(VectorIndexOption(dimensions = 5))

        assertTrue(validate(node(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)), index).isEmpty())
    }

    @Test
    fun `fail when the index has no options`() {
        val issues = validate(node(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)), vectorIndex(null))

        assertEquals(1, issues.size)
        assertEquals("missing_vector_index_dimension", issues.first().code)
        assertEquals("nodes.person.extensions.indexes.idx0", issues.first().path)
    }

    @Test
    fun `fail when the index options have no dimensions`() {
        val issues = validate(
            node(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)),
            vectorIndex(VectorIndexOption(dimensions = null))
        )

        assertEquals(1, issues.size)
        assertEquals("missing_vector_index_dimension", issues.first().code)
    }

    @Test
    fun `fail when index and property dimensions differ`() {
        val issues = validate(
            node(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)),
            vectorIndex(VectorIndexOption(dimensions = 10))
        )

        assertEquals(1, issues.size)
        assertEquals("mismatched_vector_dimension", issues.first().code)
        assertEquals(
            "Index dimension (10) does not match property dimension (5). " +
                "Update the property dimension to sync the index dimension.",
            issues.first().message
        )
    }
}
