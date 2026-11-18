// Generated from org/neo4j/spec/graph/validate/Validation.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate;

import java.util.Collections;
import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;

public interface Validation {
    default List<Validation> dependsOn() {
        return Collections.emptyList();
    }

    void validate(GraphModel model, List<Issue> issues);
}
