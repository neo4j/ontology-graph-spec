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
package org.neo4j.spec.graph.validate.labelled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks every relationship has a type, which isn't blank. Reports {@code unset_relationship_type} at the
 * relationship's {@code type}.
 */
public final class RelationshipTypeValidator implements Validator {

    static final RelationshipTypeValidator INSTANCE = new RelationshipTypeValidator();

    private RelationshipTypeValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Relationship> entry : model.relationships().entrySet()) {
            if (Schema.isBlank(entry.getValue().type())) {
                issues.add(Issue.of(
                        "unset_relationship_type",
                        "The relationship '" + entry.getKey() + "' has no type",
                        "relationships." + entry.getKey() + ".type"));
            }
        }
        return issues;
    }

    @Override
    public String toString() {
        return "RelationshipTypeValidator";
    }
}
