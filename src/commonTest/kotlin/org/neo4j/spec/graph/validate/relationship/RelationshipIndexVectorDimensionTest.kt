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
import org.neo4j.spec.graph.model.index.VectorIndexOption
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipIndexVectorDimensionTest {

    private val validator = RelationshipIndexVectorDimension
    private val model = GraphModel(version = "1", id = "test")

    private fun relationship(property: Property) = Relationship(
        type = "KNOWS",
        from = RelationshipTarget(node = "node0"),
        to = RelationshipTarget(node = "node0"),
        properties = mutableMapOf("embedding" to property)
    )

    private fun validate(relationship: Relationship, index: RelationshipIndex): MutableList<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, "rel0", relationship, "idx0", index, issues)
        return issues
    }

    private fun vectorIndex(options: VectorIndexOption?) = RelationshipIndex(
        type = IndexType.VECTOR,
        properties = mutableSetOf("embedding"),
        options = options
    )

    @Test
    fun `pass when index is not a vector index`() {
        val index = RelationshipIndex(type = IndexType.RANGE, properties = mutableSetOf("embedding"))

        assertTrue(validate(relationship(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)), index).isEmpty())
    }

    @Test
    fun `pass when index and property dimensions match`() {
        val index = vectorIndex(VectorIndexOption(dimensions = 5))

        assertTrue(validate(relationship(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)), index).isEmpty())
    }

    @Test
    fun `fail when the index options have no dimensions`() {
        val issues = validate(
            relationship(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)),
            vectorIndex(VectorIndexOption(dimensions = null))
        )

        // Replaced legacy code: MISSING_VECTOR_INDEX_DIMENSION
        assertEquals(1, issues.size)
        assertEquals("missing_vector_index_dimension", issues.first().code)
        assertEquals("/relationships/rel0/extensions/indexes/idx0", issues.first().path)
    }

    @Test
    fun `fail when index and property dimensions differ`() {
        val issues = validate(
            relationship(Property(type = Neo4jType.VECTOR_FLOAT, dimension = 5)),
            vectorIndex(VectorIndexOption(dimensions = 10))
        )

        // Replaced legacy code: MISMATCHED_VECTOR_DIMENSION
        assertEquals(1, issues.size)
        assertEquals("mismatched_vector_dimension", issues.first().code)
    }
}
