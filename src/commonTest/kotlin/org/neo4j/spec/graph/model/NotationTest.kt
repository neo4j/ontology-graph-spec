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
package org.neo4j.spec.graph.model

import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.node.extension.NodeConstraintExtensions
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.type.ConstraintType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotationTest {

    private fun model(
        nodes: MutableMap<String, Node> = mutableMapOf(),
        relationships: MutableMap<String, Relationship> = mutableMapOf()
    ) = GraphModel(version = "1.0", id = "test", nodes = nodes, relationships = relationships)

    @Test
    fun `moves node label to identifier`() {
        val model = model(nodes = mutableMapOf("User" to Node(label = "UserLabel")))

        model.longhand()

        val node = model.nodes["User"]
        assertNotNull(node, "Node keys should be left alone")
        assertNull(node.label, "Label should be moved to Labels object and set to null")
        assertEquals("UserLabel", node.labels.identifier)
    }

    @Test
    fun `label doesn't replace an existing identifier and is kept when they conflict`() {
        val node = Node(label = "Short")
        node.labels.identifier = "Long"
        val model = model(nodes = mutableMapOf("User" to node))

        model.longhand()

        assertEquals("Short", node.label, "A conflicting label must not be discarded")
        assertEquals("Long", node.labels.identifier)
    }

    @Test
    fun `label is cleared when it matches an existing identifier`() {
        val node = Node(label = "User")
        node.labels.identifier = "User"
        val model = model(nodes = mutableMapOf("User" to node))

        model.longhand()

        assertNull(node.label)
        assertEquals("User", node.labels.identifier)
    }

    @Test
    fun `leaves keys and references alone`() {
        val model = model(
            nodes = mutableMapOf("Person" to Node(label = "Person", properties = mutableMapOf("age" to Property()))),
            relationships = mutableMapOf(
                "FRIENDS_WITH" to Relationship(
                    type = "KNOWS",
                    from = RelationshipTarget(node = "Person"),
                    to = RelationshipTarget(node = "Person"),
                    properties = mutableMapOf("since" to Property())
                )
            )
        )

        model.longhand()

        assertTrue(model.nodes["Person"]!!.properties.containsKey("age"))
        val relationship = model.relationships["FRIENDS_WITH"]!!
        assertTrue(relationship.properties.containsKey("since"))
        assertEquals("Person", relationship.from.node)
        assertEquals("Person", relationship.to.node)
    }

    @Test
    fun `converts key property flag into a node key constraint`() {
        val model = model(
            nodes = mutableMapOf(
                "User" to Node(label = "User", properties = mutableMapOf("id" to Property(key = true)))
            )
        )

        model.longhand()

        val node = model.nodes["User"]!!
        assertNull(node.properties["id"]!!.key, "Key flag should be cleared from the property")

        val constraint = node.constraints["key_User_id"]
        assertNotNull(constraint, "A key constraint should be generated for the property")
        assertEquals(ConstraintType.KEY, constraint.type)
        assertEquals(mutableListOf("id"), constraint.properties)
        assertEquals("id_User_key", constraint.name)
    }

    @Test
    fun `converts unique property flag into a node unique constraint`() {
        val model = model(
            nodes = mutableMapOf("User" to Node(properties = mutableMapOf("email" to Property(unique = true))))
        )

        model.longhand()

        val node = model.nodes["User"]!!
        assertNull(node.properties["email"]!!.unique, "Unique flag should be cleared from the property")

        val constraint = node.constraints["unique_User_email"]
        assertNotNull(constraint, "A unique constraint should be generated for the property")
        assertEquals(ConstraintType.UNIQUE, constraint.type)
        assertEquals(mutableListOf("email"), constraint.properties)
        assertEquals("email_uniq", constraint.name)
    }

    @Test
    fun `converts mustExist property flag into a node exists constraint`() {
        val model = model(
            nodes = mutableMapOf("User" to Node(properties = mutableMapOf("email" to Property(mustExist = true))))
        )

        model.longhand()

        val node = model.nodes["User"]!!
        assertNull(node.properties["email"]!!.mustExist, "MustExist flag should be cleared from the property")

        val constraint = node.constraints["exists_User_email"]
        assertNotNull(constraint, "An exists constraint should be generated for the property")
        assertEquals(ConstraintType.EXISTS, constraint.type)
        assertEquals(mutableListOf("email"), constraint.properties)
        assertEquals("email_exists", constraint.name)
    }

    @Test
    fun `uses the property name as the constraint name token`() {
        val model = model(
            nodes = mutableMapOf(
                "User" to Node(
                    label = "User",
                    properties = mutableMapOf("p0" to Property(name = "email", unique = true))
                )
            )
        )

        model.longhand()

        val constraint = model.nodes["User"]!!.constraints["unique_User_email"]!!
        assertEquals(mutableListOf("p0"), constraint.properties)
        assertEquals("email_User_uniq", constraint.name)
    }

    @Test
    fun `keeps an explicit node constraint alongside a shorthand one`() {
        val model = model(
            nodes = mutableMapOf(
                "User" to Node(
                    constraints = mutableMapOf(
                        "c1" to NodeConstraint(ConstraintType.UNIQUE, properties = mutableListOf(), name = "custom")
                    ),
                    properties = mutableMapOf("id" to Property(key = true))
                )
            )
        )

        model.longhand()

        val constraints = model.nodes["User"]!!.constraints
        assertEquals(2, constraints.size, "Both constraints should survive")
        assertEquals("custom", constraints["c1"]?.name)
        assertEquals(ConstraintType.KEY, constraints["key_User_id"]?.type)
    }

    @Test
    fun `converts key property flag into a relationship key constraint`() {
        val model = model(
            relationships = mutableMapOf(
                "KNOWS" to Relationship(
                    type = "KNOWS",
                    from = RelationshipTarget(),
                    to = RelationshipTarget(),
                    properties = mutableMapOf("since" to Property(key = true))
                )
            )
        )

        model.longhand()

        val relationship = model.relationships["KNOWS"]!!
        assertNull(relationship.properties["since"]!!.key, "Key flag should be cleared from the property")

        val constraint: RelationshipConstraint? = relationship.constraints["key_KNOWS_since"]
        assertNotNull(constraint, "A key constraint should be generated for the property")
        assertEquals(ConstraintType.KEY, constraint.type)
        assertEquals(mutableListOf("since"), constraint.properties)
        assertEquals("since_KNOWS_key", constraint.name)
    }

    @Test
    fun `leaves properties without flags unconstrained`() {
        val model = model(nodes = mutableMapOf("User" to Node(properties = mutableMapOf("name" to Property()))))

        model.longhand()

        assertTrue(model.nodes["User"]!!.constraints.isEmpty())
    }

    @Test
    fun `longhand twice is the same as once`() {
        val model = model(
            nodes = mutableMapOf(
                "User" to Node(
                    label = "User",
                    properties = mutableMapOf("id" to Property(key = true), "email" to Property(unique = true))
                )
            )
        )

        model.longhand()
        val once = model.copy(nodes = model.nodes.toMutableMap())
        model.longhand()

        assertEquals(once, model)
        assertEquals(setOf("key_User_id", "unique_User_email"), model.nodes["User"]!!.constraints.keys)
    }

    @Test
    fun `shorthand reverses longhand`() {
        val model = model(
            nodes = mutableMapOf(
                "User" to Node(
                    label = "User",
                    properties = mutableMapOf(
                        "id" to Property(key = true),
                        "email" to Property(unique = true, mustExist = true)
                    )
                )
            ),
            relationships = mutableMapOf(
                "KNOWS" to Relationship(
                    type = "KNOWS",
                    from = RelationshipTarget(),
                    to = RelationshipTarget(),
                    properties = mutableMapOf("since" to Property(key = true))
                )
            )
        )
        val original = model.copy(
            nodes = model.nodes.mapValues { (_, n) ->
                n.copy(properties = n.properties.mapValues { it.value.copy() }.toMutableMap())
            }
                .toMutableMap()
        )

        model.longhand()
        model.shorthand()

        assertEquals("User", model.nodes["User"]!!.label)
        assertNull(model.nodes["User"]!!.labels.identifier)
        assertTrue(model.nodes["User"]!!.constraints.isEmpty())
        assertTrue(model.relationships["KNOWS"]!!.constraints.isEmpty())
        assertEquals(original.nodes["User"]!!.properties, model.nodes["User"]!!.properties)
        assertEquals(original.relationships["KNOWS"]!!.properties, model.relationships["KNOWS"]!!.properties)
    }

    @Test
    fun `shorthand keeps what it can't represent`() {
        val node = Node(
            properties = mutableMapOf("id" to Property(), "a" to Property(), "b" to Property()),
            constraints = mutableMapOf(
                "custom" to NodeConstraint(ConstraintType.UNIQUE, mutableListOf("id"), name = "my_name"),
                "composite" to NodeConstraint(ConstraintType.KEY, mutableListOf("a", "b")),
                "extended" to NodeConstraint(
                    ConstraintType.EXISTS,
                    mutableListOf("b"),
                    NodeConstraintExtensions(custom = mutableMapOf("owner" to "team"))
                )
            )
        )
        node.labels.identifier = "User"
        node.labels.implied.add("Person")
        val model = model(nodes = mutableMapOf("User" to node))

        model.shorthand()

        assertEquals(3, node.constraints.size)
        assertEquals("User", node.labels.identifier)
        assertNull(node.label)
        assertNull(node.properties["id"]!!.unique)
    }

    @Test
    fun `longhand leaves a node without a label or identifier alone`() {
        val node = Node()
        val model = model(nodes = mutableMapOf("User" to node))

        model.longhand()

        assertNull(node.label)
        assertNull(node.labels.identifier)
    }
}
