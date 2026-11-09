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
import org.neo4j.spec.graph.validate.IssueKeyType
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.pathOf
import kotlin.js.JsExport
@JsExport
object RelationshipIndexes : RelationshipValidation {
    override fun validateIndex(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        indexId: String,
        index: RelationshipIndex,
        issues: MutableList<Issue>
    ) {
        for (property in index.properties) {
            if (!relationship.properties.containsKey(property)) {
                issues.add(
                    Issue(
                        code = "missing_relation_index_property",
                        message = "Missing property with id '$property' for relationship index '$indexId'",
                        path = pathOf(
                            "relationships",
                            relationshipId,
                            "extensions",
                            "indexes",
                            indexId,
                            "properties",
                            property
                        ),
                        details = issueDetails(IssueKeyType.INDEX, indexId)
                    )
                )
            }
        }
    }
}
