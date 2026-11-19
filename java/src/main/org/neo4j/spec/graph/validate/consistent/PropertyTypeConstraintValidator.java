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
package org.neo4j.spec.graph.validate.consistent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.ConstraintType;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks the property of a {@code PROPERTY_TYPE} constraint has a type, since the constraint enforces it. A type of
 * {@code ANY} counts as no type. Reports {@code node_type_constraint_property_untyped} and
 * {@code relationship_type_constraint_property_untyped} at the constraint's {@code properties[0]}.
 */
public final class PropertyTypeConstraintValidator implements Validator {

    static final PropertyTypeConstraintValidator INSTANCE = new PropertyTypeConstraintValidator();

    private PropertyTypeConstraintValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Schema.Constraint constraint : Schema.constraints(model)) {
            if (constraint.type() != ConstraintType.PROPERTY_TYPE
                    || constraint.properties().size() != 1) {
                continue;
            }
            String id = constraint.properties().get(0);
            Property property = constraint.available().get(id);
            if (property != null && !property.type().isPresent()) {
                issues.add(Issue.of(
                        constraint.owner() + "_type_constraint_property_untyped",
                        "The PROPERTY_TYPE constraint '" + constraint.id() + "' is on the property '" + id
                                + "', which has no type",
                        constraint.path() + ".properties[0]"));
            }
        }
        return issues;
    }

    @Override
    public Set<Validator> dependsOn() {
        return Dependencies.core();
    }

    @Override
    public String toString() {
        return "PropertyTypeConstraintValidator";
    }
}
