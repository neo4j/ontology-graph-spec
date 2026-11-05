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
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeLabelDuplicateOptionalTest {

    private fun validate(values: MutableList<String>): List<Issue> {
        val model = GraphModel(
        version = "1",
        id = "test",
        nodes = mutableMapOf("n" to Node(labels = Labels(optional = values)))
    )
        return mutableListOf<Issue>().also { NodeLabelDuplicate.validate(model, it) }
    }

    @Test
    fun `pass when entries are unique`() {
        assertTrue(validate(mutableListOf("a", "b")).isEmpty())
    }

    @Test
    fun `fail on repeated entry`() {
        val issues = validate(mutableListOf("a", "a", "b"))

        assertEquals(1, issues.size)
        assertEquals("duplicate_optional_label", issues.first().code)
        assertEquals("nodes.n.labels.optional[1]", issues.first().path)
        assertEquals("a", issues.first().details["value"])
    }
}
