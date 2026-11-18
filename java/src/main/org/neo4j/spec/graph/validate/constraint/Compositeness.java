// Generated from org/neo4j/spec/graph/validate/constraint/Compositeness.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.constraint;

import java.util.HashSet;
import java.util.List;

public final class Compositeness {
    private Compositeness() {
    }

    public static boolean isCompositeConstraint(List<String> properties) {
        return new HashSet<>(properties).size() >= 2;
    }
}
