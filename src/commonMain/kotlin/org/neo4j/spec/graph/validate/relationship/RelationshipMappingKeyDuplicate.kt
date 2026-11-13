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
package org.neo4j.spec.graph.validate.relationship

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.duplicate.duplicateEntryIssues

/**
 * A relationship mapping key must list each property once.
 */
object RelationshipMappingKeyDuplicate : RelationshipValidation {
    override fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
        for ((mappingId, mappings) in model.extensions.dataSourceMappings) {
            for ((index, mapping) in mappings.withIndex()) {
                if (mapping !is RelationshipMapping || mapping.relationship != relationshipId) {
                    continue
                }
                issues += duplicateEntryIssues(
                    code = "duplicate_relationship_mapping_key",
                    kind = "mapping key",
                    owner = "relationship '$relationshipId'",
                    values = mapping.key,
                    path = arrayOf<Any>("extensions", "data_source_mappings", mappingId, index, "key")
                )
            }
        }
    }
}
