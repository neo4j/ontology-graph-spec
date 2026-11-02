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
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipPropertyEmptyNameTest {

    private val validator = RelationshipPropertyEmptyName
    private val model =
        GraphModel(version = "1", id = "test")
    private val target = RelationshipTarget(node = "n:1", label = "Person")

    @Test
    fun `pass when name is blank and the key is the name`() {
        val rel = Relationship(
            type = "KNOWS",
            from = target,
            to = target,
            properties = mutableMapOf("p:1" to Property(name = ""))
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "r:1", rel, "p:1", rel.properties["p:1"]!!, issues)

        assertTrue(issues.isEmpty(), "Expected no issues; a blank name override falls back to the key")
    }

    @Test
    fun `pass when name is omitted and the key is the name`() {
        val rel = Relationship(
            type = "KNOWS",
            from = target,
            to = target,
            properties = mutableMapOf("p:1" to Property(name = null))
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "r:1", rel, "p:1", rel.properties["p:1"]!!, issues)

        assertTrue(issues.isEmpty(), "Expected no issues; the map key is the property name")
    }

    @Test
    fun `fail when the key is blank and no name is set`() {
        val rel = Relationship(
            type = "KNOWS",
            from = target,
            to = target,
            properties = mutableMapOf("" to Property())
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "r:1", rel, "", rel.properties[""]!!, issues)

        assertEquals(1, issues.size)
        assertEquals("missing_property_name", issues.first().code)
    }

    @Test
    fun `pass when property has a valid name`() {
        val rel = Relationship(
            type = "KNOWS",
            from = target,
            to = target,
            properties = mutableMapOf("p:1" to Property(name = "since"))
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "r:1", rel, "p:1", rel.properties["p:1"]!!, issues)

        assertTrue(issues.isEmpty())
    }
}
