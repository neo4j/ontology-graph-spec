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
import org.neo4j.spec.graph.model.index.FullTextIndexOption
import org.neo4j.spec.graph.model.index.PointIndexOption
import org.neo4j.spec.graph.model.index.VectorIndexOption
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeIndexOptionsTest {

    private val validator = NodeIndexOptions
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `no options set - no issues`() {
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
            properties = mutableListOf("email", "status"),
            options = null
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, nodeId, node, indexId, index, issues)

        assertTrue(issues.isEmpty(), "Expected no validation issues when options is null")
    }

    @Test
    fun `options type matches index type - no issues`() {
        val nodeId = "userNode"
        val indexId = "idx_user_email_status"

        val node = Node(
            properties = mutableMapOf(
                "email" to Property(),
                "status" to Property()
            )
        )
        val index = NodeIndex(
            type = IndexType.FULLTEXT,
            labels = mutableListOf(),
            properties = mutableListOf("email", "status"),
            options = FullTextIndexOption()
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, nodeId, node, indexId, index, issues)

        assertTrue(issues.isEmpty(), "Expected no validation issues when options type matches index type")
    }

    @Test
    fun `options type does not match index type - issue reported`() {
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
            properties = mutableListOf("email", "status"),
            options = PointIndexOption()
        )

        val issues = mutableListOf<Issue>()
        validator.validateIndex(model, nodeId, node, indexId, index, issues)

        assertEquals(1, issues.size, "Expected exactly one validation issue")

        val issue = issues.first()
        assertEquals("node_index_type_options_mismatch", issue.code)
        assertEquals(
            "Cannot use options type 'POINT' with node index 'idx_user_email_status' type 'TEXT'",
            issue.message
        )
        assertEquals("nodes.userNode.extensions.indexes.idx_user_email_status.options", issue.path)
    }

    @Test
    fun `any mismatched type combination reports an issue`() {
        for (indexType in IndexType.entries) {
            for (options in listOf(FullTextIndexOption(), PointIndexOption(), VectorIndexOption())) {
                if (options.type == indexType) {
                    continue
                }
                val nodeId = "userNode"
                val indexId = "idx_mismatch"

                val node = Node(properties = mutableMapOf())
                val index = NodeIndex(
                    type = indexType,
                    labels = mutableListOf(),
                    properties = mutableListOf(),
                    options = options
                )

                val issues = mutableListOf<Issue>()
                validator.validateIndex(model, nodeId, node, indexId, index, issues)

                assertEquals(1, issues.size, "Expected a mismatch issue for $indexType vs $options")
                assertEquals("node_index_type_options_mismatch", issues.first().code)
            }
        }
    }

    @Test
    fun `matching type for every IndexType produces no issues`() {
        for (indexType in IndexType.entries) {
            for (options in listOf(FullTextIndexOption(), PointIndexOption(), VectorIndexOption())) {
                if (options.type != indexType) {
                    continue
                }
                val nodeId = "userNode"
                val indexId = "idx_match"

                val node = Node(properties = mutableMapOf())
                val index = NodeIndex(
                    type = indexType,
                    labels = mutableListOf(),
                    properties = mutableListOf(),
                    options = options
                )

                val issues = mutableListOf<Issue>()
                validator.validateIndex(model, nodeId, node, indexId, index, issues)

                assertTrue(issues.isEmpty(), "Expected no issues when options type equals index type ($indexType)")
            }
        }
    }
}
