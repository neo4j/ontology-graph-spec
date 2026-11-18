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
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipIndexOptions implements RelationshipValidation {
    public static final RelationshipIndexOptions INSTANCE = new RelationshipIndexOptions();

    private RelationshipIndexOptions() {}

    @Override
    public void validateIndex(
            GraphModel model,
            String relationshipId,
            Relationship relationship,
            String indexId,
            RelationshipIndex index,
            List<Issue> issues) {
        if (index.getOptions() != null && index.getOptions().getType() != index.getType()) {
            issues.add(new Issue(
                    "relationship_index_type_options_mismatch",
                    "Cannot use options type '" + index.getOptions().getType() + "' with relationship index '" + indexId
                            + "' " + ("type '" + index.getType() + "'"),
                    "relationships." + relationshipId + ".extensions.indexes." + indexId + ".options"));
        }
    }
}
