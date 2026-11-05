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
package org.neo4j.spec.graph.validate.constraint

import org.neo4j.spec.graph.model.node.Constraint

/**
 * Returns ids of the other constraints that duplicate [constraintId].
 * Duplicates are constraints on the same properties with the same type or, when composite, any type.
 */
internal fun <C : Constraint> duplicateConstraintIds(constraintId: String, constraints: Map<String, C>): List<String> {
    val constraint = constraints.getValue(constraintId)
    return constraints.filter { (otherId, other) ->
        otherId != constraintId &&
            other.properties.toSet() == constraint.properties.toSet() &&
            (other.type == constraint.type || isCompositeConstraint(constraint.properties))
    }.keys.toList()
}
