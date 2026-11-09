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
package org.neo4j.spec.graph.validate.length

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.node.extension.NodeExtensions
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.length.StringLength.MAX_CONSTRAINT_INDEX_NAME_LENGTH
import org.neo4j.spec.graph.validate.length.StringLength.MAX_DESCRIPTION_LENGTH
import org.neo4j.spec.graph.validate.length.StringLength.MAX_MODEL_DESCRIPTION_LENGTH
import org.neo4j.spec.graph.validate.length.StringLength.MAX_NAME_LENGTH
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringLengthTest {

    private fun model(
        name: String = "",
        description: String = "",
        nodes: Map<String, Node> = emptyMap(),
        relationships: Map<String, Relationship> = emptyMap()
    ) = GraphModel(
        version = "1",
        id = "test",
        name = name,
        description = description,
        nodes = nodes.toMutableMap(),
        relationships = relationships.toMutableMap()
    )

    private fun validate(model: GraphModel): List<Issue> =
        mutableListOf<Issue>().also { StringLength.validate(model, it) }

    private fun paths(model: GraphModel) = validate(model).map { it.path }

    @Test
    fun `pass at the limits`() {
        val node = Node(
            description = "a".repeat(MAX_DESCRIPTION_LENGTH),
            properties = mutableMapOf(
                "p" to Property(name = "a".repeat(MAX_NAME_LENGTH), description = "a".repeat(MAX_DESCRIPTION_LENGTH))
            ),
            constraints = mutableMapOf(
                "c" to NodeConstraint(
                    ConstraintType.UNIQUE,
                    mutableSetOf("p"),
                    name = "a".repeat(MAX_CONSTRAINT_INDEX_NAME_LENGTH)
                )
            ),
            extensions = NodeExtensions(
                indexes = mutableMapOf(
                    "i" to NodeIndex(
                        IndexType.RANGE,
                        mutableSetOf("L"),
                        mutableSetOf("p"),
                        name = "a".repeat(MAX_CONSTRAINT_INDEX_NAME_LENGTH)
                    )
                )
            )
        )
        val model = model(
            description = "a".repeat(MAX_MODEL_DESCRIPTION_LENGTH),
            nodes = mapOf("n" to node)
        )

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `fail when model name or description is too long`() {
        val model = model(
            name = "a".repeat(MAX_NAME_LENGTH + 1),
            description = "a".repeat(MAX_MODEL_DESCRIPTION_LENGTH + 1)
        )

        assertEquals(listOf<String?>("/name", "/description"), paths(model))
        assertEquals(listOf("name_too_long", "description_too_long"), validate(model).map { it.code })
    }

    @Test
    fun `fail when node fields are too long`() {
        val node = Node(
            description = "a".repeat(MAX_DESCRIPTION_LENGTH + 1),
            properties = mutableMapOf(
                "p" to Property(
                    name = "a".repeat(MAX_NAME_LENGTH + 1),
                    description = "a".repeat(MAX_DESCRIPTION_LENGTH + 1)
                )
            ),
            constraints = mutableMapOf(
                "c" to NodeConstraint(
                    ConstraintType.UNIQUE,
                    mutableSetOf("p"),
                    name = "a".repeat(MAX_CONSTRAINT_INDEX_NAME_LENGTH + 1)
                )
            ),
            extensions = NodeExtensions(
                indexes = mutableMapOf(
                    "i" to NodeIndex(
                        IndexType.RANGE,
                        mutableSetOf("L"),
                        mutableSetOf("p"),
                        name = "a".repeat(MAX_CONSTRAINT_INDEX_NAME_LENGTH + 1)
                    )
                )
            )
        )

        assertEquals(
            listOf<String?>(
                "/nodes/n/description",
                "/nodes/n/properties/p/name",
                "/nodes/n/properties/p/description",
                "/nodes/n/constraints/c/name",
                "/nodes/n/extensions/indexes/i/name"
            ),
            paths(model(nodes = mapOf("n" to node)))
        )
    }

    @Test
    fun `fail when relationship fields are too long`() {
        val relationship = Relationship(
            type = "KNOWS",
            from = RelationshipTarget(),
            to = RelationshipTarget(),
            description = "a".repeat(MAX_DESCRIPTION_LENGTH + 1),
            constraints = mutableMapOf(
                "c" to RelationshipConstraint(
                    ConstraintType.UNIQUE,
                    mutableSetOf("p"),
                    name = "a".repeat(MAX_CONSTRAINT_INDEX_NAME_LENGTH + 1)
                )
            )
        )

        assertEquals(
            listOf<String?>(
                "/relationships/r/description",
                "/relationships/r/constraints/c/name"
            ),
            paths(model(relationships = mapOf("r" to relationship)))
        )
    }

    @Test
    fun `constraint names allow more than regular names`() {
        val node = Node(
            constraints = mutableMapOf(
                "c" to
                    NodeConstraint(
                        ConstraintType.UNIQUE,
                        mutableSetOf("p"),
                        name = "a".repeat(MAX_NAME_LENGTH + 1)
                    )
            )
        )

        assertTrue(validate(model(nodes = mapOf("n" to node))).isEmpty())
    }
}
