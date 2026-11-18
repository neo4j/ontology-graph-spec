// Generated from org/neo4j/spec/graph/model/node/Constraint.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.node;

import java.util.List;
import org.neo4j.spec.graph.model.type.ConstraintType;

public interface Constraint {
    ConstraintType getType();

    List<String> getProperties();
}
