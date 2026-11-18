// Generated from org/neo4j/spec/graph/validate/node/NodeIndexConstraintNameConflict.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.Kt;
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

    private NodeIndexConstraintNameConflict() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        Set<String> duplicateNames = duplicateNames(model);
        for (var entry : node.getExtensions().getIndexes().entrySet()) {
            var indexId = entry.getKey();
            var index = entry.getValue();
            String name = index.getName();
            if (!Kt.isNullOrEmpty(name) && duplicateNames.contains(name)) {
                issues.add(new Issue("duplicate_node_index_constraint_name", "Index '" + indexId + "' on node '" + nodeId + "' reuses name '" + name + "' " + "already used by another index or constraint", "nodes." + nodeId + ".extensions.indexes." + indexId + ".name"));
            }
        }
        for (var entry : node.getConstraints().entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            String name = constraint.getName();
            if (!Kt.isNullOrEmpty(name) && duplicateNames.contains(name)) {
                issues.add(new Issue("duplicate_node_index_constraint_name", "Constraint '" + constraintId + "' on node '" + nodeId + "' reuses name '" + name + "' " + "already used by another index or constraint", "nodes." + nodeId + ".constraints." + constraintId + ".name"));
            }
        }
    }

    private Set<String> duplicateNames(GraphModel model) {
        Map<String, Integer> seen = new LinkedHashMap<String, Integer>();
        for (var node : model.getNodes().values()) {
            for (var index : node.getExtensions().getIndexes().values()) {
                String name = index.getName();
                if (!Kt.isNullOrEmpty(name)) {
                    Integer value = seen.get(name);
                    seen.put(name, (value != null ? value : 0) + 1);
                }
            }
            for (var constraint : node.getConstraints().values()) {
                String name = constraint.getName();
                if (!Kt.isNullOrEmpty(name)) {
                    Integer value = seen.get(name);
                    seen.put(name, (value != null ? value : 0) + 1);
                }
            }
        }
        return Kt.filterValues(seen, it -> it > 1).keySet();
    }
}
