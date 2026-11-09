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
import org.neo4j.spec.graph.validate.issueDetails
import kotlin.js.JsExport

/**
 * Restricts the length of names and descriptions throughout the model.
 */
@JsExport
object StringLength : Validation {

    const val MAX_NAME_LENGTH = 256
    const val MAX_CONSTRAINT_INDEX_NAME_LENGTH = 800
    const val MAX_MODEL_DESCRIPTION_LENGTH = 64 * 1024
    const val MAX_DESCRIPTION_LENGTH = 1024

    override fun validate(model: GraphModel, issues: MutableList<Issue>) {
        check("name", model.name, MAX_NAME_LENGTH, "name", "model", model.id, issues)
        check("description", model.description, MAX_MODEL_DESCRIPTION_LENGTH, "description", "model", model.id, issues)

        for ((nodeId, node) in model.nodes) {
            checkEntity(
                path = "nodes.$nodeId",
                entityType = "node",
                entityId = nodeId,
                description = node.description,
                properties = node.properties,
                constraints = node.constraints,
                indexes = node.extensions.indexes,
                issues = issues
            )
        }

        for ((relationshipId, relationship) in model.relationships) {
            checkEntity(
                path = "relationships.$relationshipId",
                entityType = "relationship",
                entityId = relationshipId,
                description = relationship.description,
                properties = relationship.properties,
                constraints = relationship.constraints,
                indexes = relationship.extensions.indexes,
                issues = issues
            )
        }
    }

    private fun checkEntity(
        path: String,
        entityType: String,
        entityId: String,
        description: String,
        properties: Map<String, Property>,
        constraints: Map<String, Named>,
        indexes: Map<String, Named>,
        issues: MutableList<Issue>
    ) {
        check("description", description, MAX_DESCRIPTION_LENGTH, "$path.description", entityType, entityId, issues)
        for ((propertyId, property) in properties) {
            val propertyPath = "$path.properties.$propertyId"
            check("name", property.name, MAX_NAME_LENGTH, "$propertyPath.name", "property", propertyId, issues)
            check(
                "description",
                property.description,
                MAX_DESCRIPTION_LENGTH,
                "$propertyPath.description",
                "property",
                propertyId,
                issues
            )
        }
        for ((constraintId, constraint) in constraints) {
            val constraintPath = "$path.constraints.$constraintId.name"
            check(
                "name",
                constraint.name,
                MAX_CONSTRAINT_INDEX_NAME_LENGTH,
                constraintPath,
                "constraint",
                constraintId,
                issues
            )
        }
        for ((indexId, index) in indexes) {
            val indexPath = "$path.extensions.indexes.$indexId.name"
            check("name", index.name, MAX_CONSTRAINT_INDEX_NAME_LENGTH, indexPath, "index", indexId, issues)
        }
    }

    private fun check(
        field: String,
        value: String?,
        max: Int,
        path: String,
        keyType: String,
        key: String,
        issues: MutableList<Issue>
    ) {
        if (value != null && value.length > max) {
            issues.add(
                Issue(
                    code = "${field}_too_long",
                    message = "The $field at '$path' is ${value.length} characters long, the maximum is $max",
                    path = path,
                    details = mapOf("length" to value.length.toString(), "max" to max.toString()) +
                        issueDetails(keyType, key)
                )
            )
        }
    }
}
