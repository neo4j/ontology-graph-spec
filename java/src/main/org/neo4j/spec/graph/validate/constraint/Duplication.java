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
package org.neo4j.spec.graph.validate.constraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.neo4j.spec.graph.model.node.Constraint;

public final class Duplication {
    private Duplication() {}

    /**
     * Returns ids of the other constraints that duplicate {@code constraintId}.
     * Duplicates are constraints on the same properties with the same type or, when composite, any type.
     */
    public static <C extends Constraint> List<String> duplicateConstraintIds(
            String constraintId, Map<String, ? extends C> constraints) {
        C constraint = constraints.get(constraintId);
        Set<String> properties = new HashSet<>(constraint.getProperties());
        List<String> duplicates = new ArrayList<>();
        for (var entry : constraints.entrySet()) {
            var other = entry.getValue();
            if (!Objects.equals(entry.getKey(), constraintId)
                    && properties.equals(new HashSet<>(other.getProperties()))
                    && (other.getType() == constraint.getType()
                            || Compositeness.isCompositeConstraint(constraint.getProperties()))) {
                duplicates.add(entry.getKey());
            }
        }
        return duplicates;
    }
}
