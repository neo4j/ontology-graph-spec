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
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.Named
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.Validation
import org.neo4j.spec.graph.validate.jsonPointerOf

/**
 * Restricts the length of names and descriptions throughout the model.
 */
object StringLength : Validation {

    const val MAX_NAME_LENGTH = 256
    const val MAX_CONSTRAINT_INDEX_NAME_LENGTH = 800
    const val MAX_MODEL_DESCRIPTION_LENGTH = 64 * 1024
    const val MAX_DESCRIPTION_LENGTH = 1024

    override fun validate(model: GraphModel, issues: MutableList<Issue>) {
        check("name", model.name, MAX_NAME_LENGTH, issues, "name")
        check("description", model.description, MAX_MODEL_DESCRIPTION_LENGTH, issues, "description")

        for ((nodeId, node) in model.nodes) {
            checkEntity(
                description = node.description,
                properties = node.properties,
                constraints = node.constraints,
                indexes = node.extensions.indexes,
                issues = issues,
                path = listOf("nodes", nodeId)
            )
        }

        for ((relationshipId, relationship) in model.relationships) {
            checkEntity(
                description = relationship.description,
                properties = relationship.properties,
                constraints = relationship.constraints,
                indexes = relationship.extensions.indexes,
                issues = issues,
                path = listOf("relationships", relationshipId)
            )
        }
    }

    private fun checkEntity(
        description: String,
        properties: Map<String, Property>,
        constraints: Map<String, Named>,
        indexes: Map<String, Named>,
        issues: MutableList<Issue>,
        path: List<Any>
    ) {
        check("description", description, MAX_DESCRIPTION_LENGTH, issues, *path.toTypedArray(), "description")
        for ((propertyId, property) in properties) {
            val propertyPath = (path + "properties" + propertyId).toTypedArray()
            check("name", property.name, MAX_NAME_LENGTH, issues, *propertyPath, "name")
            check("description", property.description, MAX_DESCRIPTION_LENGTH, issues, *propertyPath, "description")
        }
        for ((constraintId, constraint) in constraints) {
            val constraintPath = (path + "constraints" + constraintId).toTypedArray()
            check("name", constraint.name, MAX_CONSTRAINT_INDEX_NAME_LENGTH, issues, *constraintPath, "name")
        }
        for ((indexId, index) in indexes) {
            val indexPath = (path + "extensions" + "indexes" + indexId).toTypedArray()
            check("name", index.name, MAX_CONSTRAINT_INDEX_NAME_LENGTH, issues, *indexPath, "name")
        }
    }

    private fun check(field: String, value: String?, max: Int, issues: MutableList<Issue>, vararg path: Any) {
        if (value != null && value.length > max) {
            val pointer = jsonPointerOf(*path)
            issues.add(
                Issue(
                    code = "${field}_too_long",
                    message = "The $field at '$pointer' is ${value.length} characters long, the maximum is $max",
                    path = pointer,
                    details = mapOf("length" to value.length.toString(), "max" to max.toString())
                )
            )
        }
    }
}
