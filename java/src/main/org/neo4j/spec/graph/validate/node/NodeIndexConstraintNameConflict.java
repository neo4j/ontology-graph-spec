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
package org.neo4j.spec.graph.validate.node;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;

/**
 * Index and constraint names must be unique across the whole model. Any name used by more than one
 * index or constraint is a `duplicate_node_index_constraint_name`, attached to every index and constraint
 * carrying that name. Blank and missing names are skipped.
 *
 * Ported from UPX `getNonUniqueNames` (import-shared/src/data-model/errors.ts). Graph-spec supports
 * mapping multiple files or columns to the same node natively, so it does not carry UPX's
 * "identical definition may reuse a name" exception - all duplicate names are flagged.
 */
public final class NodeIndexConstraintNameConflict implements NodeValidation {
    public static final NodeIndexConstraintNameConflict INSTANCE = new NodeIndexConstraintNameConflict();

    private NodeIndexConstraintNameConflict() {}

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        Set<String> duplicateNames = duplicateNames(model);
        for (var entry : node.getExtensions().getIndexes().entrySet()) {
            var indexId = entry.getKey();
            var index = entry.getValue();
            String name = index.getName();
            if (name != null && !name.isBlank() && duplicateNames.contains(name)) {
                issues.add(new Issue(
                        "duplicate_node_index_constraint_name",
                        "Index '" + indexId + "' on node '" + nodeId + "' reuses name '" + name + "' "
                                + "already used by another index or constraint",
                        Issue.jsonPointerOf("nodes", nodeId, "extensions", "indexes", indexId, "name")));
            }
        }
        for (var entry : node.getConstraints().entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            String name = constraint.getName();
            if (name != null && !name.isBlank() && duplicateNames.contains(name)) {
                issues.add(new Issue(
                        "duplicate_node_index_constraint_name",
                        "Constraint '" + constraintId + "' on node '" + nodeId + "' reuses name '" + name + "' "
                                + "already used by another index or constraint",
                        Issue.jsonPointerOf("nodes", nodeId, "constraints", constraintId, "name")));
            }
        }
    }

    private Set<String> duplicateNames(GraphModel model) {
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (var node : model.getNodes().values()) {
            for (var index : node.getExtensions().getIndexes().values()) {
                String name = index.getName();
                if (name != null && !name.isBlank()) {
                    Integer value = seen.get(name);
                    seen.put(name, (value != null ? value : 0) + 1);
                }
            }
            for (var constraint : node.getConstraints().values()) {
                String name = constraint.getName();
                if (name != null && !name.isBlank()) {
                    Integer value = seen.get(name);
                    seen.put(name, (value != null ? value : 0) + 1);
                }
            }
        }
        Set<String> duplicates = new LinkedHashSet<>();
        for (var entry : seen.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(entry.getKey());
            }
        }
        return duplicates;
    }
}
