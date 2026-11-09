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
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipIndexPropertiesTest {

    private val validator = RelationshipIndexProperties
    private val model =
        GraphModel(version = "1", id = "test")
    private val targetDummy = RelationshipTarget()

    @Test
    fun `pass when index has properties`() {
        // ARRANGE
        val relationshipId = "actedIn"
        val indexId = "idx_roles"
        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf("roles" to Property())
        )
        val index = RelationshipIndex(
            type = IndexType.TEXT,
            properties = mutableSetOf("roles")
        )

        // ACT
        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, relationshipId, relationship, indexId, index, issues)

        // ASSERT
        assertTrue(issues.isEmpty(), "Expected no issues when index has properties")
    }

    @Test
    fun `fail when index has no properties`() {
        // ARRANGE
        val relationshipId = "actedIn"
        val indexId = "idx_empty"
        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf("roles" to Property())
        )
        val index = RelationshipIndex(
            type = IndexType.TEXT,
            properties = mutableSetOf()
        )

        // ACT
        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, relationshipId, relationship, indexId, index, issues)

        // ASSERT
        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("missing_index_properties", issue.code)
        assertEquals("/relationships/actedIn/extensions/indexes/idx_empty/properties", issue.path)
    }
}
