// Generated from org/neo4j/spec/graph/validate/constraint/Duplication.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.constraint;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.model.node.Constraint;

public final class DuplicationKt {
    private DuplicationKt() {
    }

    /**
     * Returns ids of the other constraints that duplicate {@code constraintId}.
     * Duplicates are constraints on the same properties with the same type or, when composite, any type.
     */
    public static <C extends Constraint> List<String> duplicateConstraintIds(String constraintId,
            Map<String, ? extends C> constraints) {
        C constraint = Kt.getValue(constraints, constraintId);
        return Kt.toList(Kt.filter(constraints, entry -> {
            var otherId = entry.getKey();
            var other = entry.getValue();
            return !Objects.equals(otherId, constraintId) && Objects.equals(Kt.toSet(other.getProperties()), Kt.toSet(constraint.getProperties())) && (other.getType() == constraint.getType() || Compositeness.isCompositeConstraint(constraint.getProperties()));
        }).keySet());
    }
}
