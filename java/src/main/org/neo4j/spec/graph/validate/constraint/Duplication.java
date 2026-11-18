// Generated from org/neo4j/spec/graph/validate/constraint/Duplication.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.constraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.neo4j.spec.graph.model.node.Constraint;

public final class Duplication {
    private Duplication() {
    }

    /**
     * Returns ids of the other constraints that duplicate {@code constraintId}.
     * Duplicates are constraints on the same properties with the same type or, when composite, any type.
     */
    public static <C extends Constraint> List<String> duplicateConstraintIds(String constraintId,
            Map<String, ? extends C> constraints) {
        C constraint = constraints.get(constraintId);
        Set<String> properties = new HashSet<>(constraint.getProperties());
        List<String> duplicates = new ArrayList<>();
        for (var entry : constraints.entrySet()) {
            var other = entry.getValue();
            if (!Objects.equals(entry.getKey(), constraintId)
                    && properties.equals(new HashSet<>(other.getProperties()))
                    && (other.getType() == constraint.getType() || Compositeness.isCompositeConstraint(constraint.getProperties()))) {
                duplicates.add(entry.getKey());
            }
        }
        return duplicates;
    }
}
