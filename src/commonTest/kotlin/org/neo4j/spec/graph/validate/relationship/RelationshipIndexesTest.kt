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

class RelationshipIndexesTest {

    private val validator = RelationshipIndexes
    private val model =
        GraphModel(version = "1", id = "test")
    private val targetDummy = RelationshipTarget()

    @Test
    fun `all index properties exist`() {
        val relationshipId = "actedIn"
        val indexId = "idx_roles"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf(
                "roles" to Property(),
                "year" to Property()
            )
        )
        val index = RelationshipIndex(
            type = IndexType.TEXT,
            properties = mutableSetOf("roles", "year")
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, relationshipId, relationship, indexId, index, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when all index properties are present on the relationship")
    }

    @Test
    fun `missing index property`() {
        val relationshipId = "actedIn"
        val indexId = "idx_roles"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf(
                "year" to Property() // Missing 'roles'
            )
        )
        val index = RelationshipIndex(
            type = IndexType.TEXT,
            properties = mutableSetOf("roles")
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, relationshipId, relationship, indexId, index, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("missing_relation_index_property", issue.code)
        assertEquals("Missing property with id 'roles' for relationship index 'idx_roles'", issue.message)
        assertEquals("/relationships/actedIn/extensions/indexes/idx_roles/properties/roles", issue.path)
    }

    @Test
    fun `missing multiple index properties`() {
        val relationshipId = "actedIn"
        val indexId = "idx_compound"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf() // Missing both properties
        )
        val index = RelationshipIndex(
            type = IndexType.TEXT,
            properties = mutableSetOf("roles", "year")
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, relationshipId, relationship, indexId, index, issues)

        assertEquals(2, issues.size)

        val missingProperties = issues.map { it.path?.substringAfterLast('/') }.toSet()
        assertEquals(setOf("roles", "year"), missingProperties)
        assertTrue(issues.all { it.code == "missing_relation_index_property" })
    }

    @Test
    fun `no properties specified`() {
        val relationshipId = "actedIn"
        val indexId = "idx_empty"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf("year" to Property())
        )
        val index = RelationshipIndex(
            type = IndexType.TEXT,
            properties = mutableSetOf()
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, relationshipId, relationship, indexId, index, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when the index does not target any properties")
    }
}
