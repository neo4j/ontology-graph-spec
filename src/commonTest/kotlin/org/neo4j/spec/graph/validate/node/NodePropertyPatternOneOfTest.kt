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
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodePropertyPatternOneOfTest {

    private val model =
        GraphModel(version = "1", id = "test")

    private fun validate(property: Property): List<Issue> {
        val node = Node(labels = Labels(identifier = "Person"), properties = mutableMapOf("p:1" to property))
        val issues = mutableListOf<Issue>()
        NodePropertyPatternOneOf.validateProperty(model, "n:1", node, "p:1", property, issues)
        return issues
    }

    @Test
    fun `pass when neither is set`() {
        assertTrue(validate(Property()).isEmpty())
    }

    @Test
    fun `pass when only pattern is set`() {
        assertTrue(validate(Property(pattern = "^a+$")).isEmpty())
    }

    @Test
    fun `pass when only one_of is set`() {
        assertTrue(validate(Property(oneOf = mutableListOf("a", "b"))).isEmpty())
    }

    @Test
    fun `fail when both are set`() {
        val issues = validate(Property(pattern = "^a+$", oneOf = mutableListOf("a")))
        assertEquals(1, issues.size)
        assertEquals("property_pattern_one_of_conflict", issues.first().code)
        assertEquals("/nodes/n:1/properties/p:1", issues.first().path)
    }
}
