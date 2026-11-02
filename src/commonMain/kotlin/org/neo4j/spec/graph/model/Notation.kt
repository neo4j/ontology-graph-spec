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

import org.neo4j.spec.graph.model.extension.CustomExtensions
import org.neo4j.spec.graph.model.node.Constraint
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.Named
import kotlin.jvm.JvmStatic

/**
 * Converts a [GraphModel] between its longhand and shorthand notations.
 * Shorthand is a more concise way of writing parts of a model on input, a model is always output in longhand.
 *  - A node's `label` is shorthand for `labels.identifier`.
 *  - A property's `key`, `unique` and `must_exist` are shorthand for constraints on that property.
 *
 * [longhand] expands shorthand, [shorthand] collapses longhand back where doing so loses no information.
 */
object Notation {
    @JvmStatic
    fun longhand(model: GraphModel) {
        model.nodes.values.forEach { node ->
            val label = node.label
            if (label != null && node.labels.identifier == null) {
                node.labels.identifier = label
            }
            if (label == node.labels.identifier) {
                node.label = null
            }
        }
        model.nodes.forEach { (key, node) ->
            expand(
                constraints = node.constraints,
                properties = node.properties,
                entity = key,
                label = node.labels.identifier ?: ""
            ) { type, props ->
                NodeConstraint(type, props)
            }
        }
        model.relationships.forEach { (key, relationship) ->
            expand(
                constraints = relationship.constraints,
                properties = relationship.properties,
                entity = key,
                label = relationship.type
            ) { type, props ->
                RelationshipConstraint(type, props)
            }
        }
    }

    @JvmStatic
    fun shorthand(model: GraphModel) {
        model.nodes.values.forEach { node ->
            collapse(node.constraints, node.properties, node.labels.identifier ?: "", { it.extensions })
            val labels = node.labels
            val identifier = labels.identifier
            if (identifier != null && labels.implied.isEmpty() && labels.optional.isEmpty() &&
                labels.extensions.custom.isEmpty()
            ) {
                node.label = identifier
                labels.identifier = null
            }
        }
        model.relationships.values.forEach { relationship ->
            collapse(relationship.constraints, relationship.properties, relationship.type, { it.extensions })
        }
    }

    /**
     * Converts single property constraints into shorthand flags on the property.
     * Constraints with a custom name or extensions are kept, as that data would be lost.
     */
    private fun <C> collapse(
        constraints: MutableMap<String, C>,
        properties: MutableMap<String, Property>,
        label: String,
        extensions: (C) -> CustomExtensions
    ) where C : Constraint, C : Named {
        val collapsed = mutableSetOf<String>()
        for ((key, constraint) in constraints) {
            val propertyKey = constraint.properties.singleOrNull() ?: continue
            val property = properties[propertyKey] ?: continue
            val expected = NameFormat.constraintName(listOf(property.name ?: propertyKey), label, constraint.type)
            if (extensions(constraint).custom.isNotEmpty() ||
                (constraint.name != null && constraint.name != expected)
            ) {
                continue
            }
            when (constraint.type) {
                ConstraintType.EXISTS -> property.mustExist = true
                ConstraintType.KEY -> property.key = true
                ConstraintType.UNIQUE -> property.unique = true
                else -> continue
            }
            collapsed.add(key)
        }
        collapsed.forEach { constraints.remove(it) }
    }

    /**
     * Converts shorthand property constraints into long-hand constraints.
     * Doesn't check or transform (e.g. for overlapping)
     */
    private fun <C> expand(
        constraints: MutableMap<String, C>,
        properties: MutableMap<String, Property>,
        entity: String,
        label: String,
        constraint: (type: ConstraintType, properties: MutableSet<String>) -> C
    ) where C : Constraint, C : Named {
        for ((key, property) in properties) {
            fun add(type: ConstraintType) {
                val token = property.name ?: key
                val created = constraint(type, mutableSetOf(key))
                val name = NameFormat.constraintName(listOf(token), label, type)
                if (name.isNotBlank()) {
                    created.name = name
                }
                constraints[deterministicId(entity, type, token)] = created
            }

            if (property.key == true) {
                property.key = null
                add(ConstraintType.KEY)
            }
            if (property.unique == true) {
                property.unique = null
                add(ConstraintType.UNIQUE)
            }
            if (property.mustExist == true) {
                property.mustExist = null
                add(ConstraintType.EXISTS)
            }
        }
    }

    internal fun deterministicId(entity: String, type: ConstraintType, vararg properties: String): String =
        "${type.name.lowercase()}_${entity}_${properties.distinct().sorted().joinToString("_")}"
}
