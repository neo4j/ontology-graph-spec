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
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.issueDetails
import kotlin.js.JsExport
@JsExport
object RelationshipIndexOptions : RelationshipValidation {
    override fun validateIndex(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        indexId: String,
        index: RelationshipIndex,
        issues: MutableList<Issue>
    ) {
        if (index.options != null && index.options.type != index.type) {
            issues.add(
                Issue(
                    code = "relationship_index_type_options_mismatch",
                    message = "Cannot use options type '${index.options.type}' with relationship index '$indexId' " +
                        "type '${index.type}'",
                    path = "relationships.$relationshipId.extensions.indexes.$indexId.options",
                    details = issueDetails("index", indexId)
                )
            )
        }
    }
}
