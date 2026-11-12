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
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.jsonPointerOf
import org.neo4j.spec.graph.validate.property.patternOneOfConflict

object NodePropertyPatternOneOf : NodeValidation {
    override fun validateProperty(
        model: GraphModel,
        nodeId: String,
        node: Node,
        propertyId: String,
        property: Property,
        issues: MutableList<Issue>
    ) {
        patternOneOfConflict(propertyId, property, jsonPointerOf("nodes", nodeId))?.let { issues.add(it) }
    }
}
