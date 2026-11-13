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
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeIndexesExistsTest {

    private val validator = NodeIndexesExists
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `all properties exist`() {
        val nodeId = "userNode"
        val indexId = "idx_user_email_status"

        val node = Node(
            properties = mutableMapOf(
                "email" to Property(),
                "status" to Property()
            )
        )
        val index = NodeIndex(
            type = IndexType.TEXT,
            labels = mutableListOf(),
            properties = mutableListOf("email", "status")
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, nodeId, node, indexId, index, issues)

        assertTrue(issues.isEmpty(), "Expected no validation issues as all index properties exist on the node")
    }

    @Test
    fun `missing single property`() {
        val nodeId = "userNode"
        val indexId = "idx_user_email"

        val node = Node(
            properties = mutableMapOf() // Missing 'email'
        )
        val index = NodeIndex(
            type = IndexType.TEXT,
            labels = mutableListOf(),
            properties = mutableListOf("email")
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, nodeId, node, indexId, index, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("missing_node_index_property", issue.code)
        assertEquals("Missing property with id 'email' for node index 'idx_user_email'", issue.message)
        assertEquals("/nodes/userNode/extensions/indexes/idx_user_email/properties/0", issue.path)
    }

    @Test
    fun `missing multiple properties`() {
        val nodeId = "userNode"
        val indexId = "idx_compound"

        val node = Node(
            properties = mutableMapOf("status" to Property()) // 'email' and 'age' are missing
        )
        val index = NodeIndex(
            type = IndexType.TEXT,
            labels = mutableListOf(),
            properties = mutableListOf("email", "status", "age")
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, nodeId, node, indexId, index, issues)

        assertEquals(2, issues.size)

        val missingPaths = issues.map { it.path }.toSet()

        assertEquals(
            setOf(
                "/nodes/userNode/extensions/indexes/idx_compound/properties/0",
                "/nodes/userNode/extensions/indexes/idx_compound/properties/2"
            ),
            missingPaths
        )
        assertTrue(issues.all { it.code == "missing_node_index_property" })
    }

    @Test
    fun `empty properties on index`() {
        val nodeId = "userNode"
        val indexId = "idx_empty"

        val node = Node(
            properties = mutableMapOf("email" to Property())
        )
        val index = NodeIndex(
            type = IndexType.TEXT,
            labels = mutableListOf(),
            properties = mutableListOf()
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, nodeId, node, indexId, index, issues)

        assertTrue(issues.isEmpty(), "An index with no properties should not generate any issues")
    }
}
