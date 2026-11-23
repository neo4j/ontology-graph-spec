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
package org.neo4j.spec.graph.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.spec.graph.model.extension.CustomExtensions;
import org.neo4j.spec.graph.model.node.Constraint;
import org.neo4j.spec.graph.model.node.Labels;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.Named;

/**
 * Converts a {@code GraphModel} between its longhand and shorthand notations.
 * Shorthand is a more concise way of writing parts of a model on input, a model is always output in longhand.
 * - A node's `label` is shorthand for `labels.identifier`.
 * - A property's `key`, `unique` and `must_exist` are shorthand for constraints on that property.
 *
 * {@code longhand} expands shorthand, {@code shorthand} collapses longhand back where doing so loses no information.
 */
public final class Notation {
    private Notation() {}

    public static void longhand(GraphModel model) {
        for (var node : model.getNodes().values()) {
            String label = node.getLabel();
            if (label != null && node.getLabels().getIdentifier() == null) {
                node.getLabels().setIdentifier(label);
            }
            if (Objects.equals(label, node.getLabels().getIdentifier())) {
                node.setLabel(null);
            }
        }
        for (var entry : model.getNodes().entrySet()) {
            var key = entry.getKey();
            var node = entry.getValue();
            String value = node.getLabels().getIdentifier();
            Notation.expand(
                    node.getConstraints(),
                    node.getProperties(),
                    key,
                    value != null ? value : "",
                    (type, props) -> NodeConstraint.builder(type, props).build());
        }
        for (var entry : model.getRelationships().entrySet()) {
            var key = entry.getKey();
            var relationship = entry.getValue();
            Notation.expand(
                    relationship.getConstraints(),
                    relationship.getProperties(),
                    key,
                    relationship.getType(),
                    (type, props) -> RelationshipConstraint.builder(type, props).build());
        }
    }

    public static void shorthand(GraphModel model) {
        for (var node : model.getNodes().values()) {
            String value = node.getLabels().getIdentifier();
            Notation.collapse(
                    node.getConstraints(), node.getProperties(), value != null ? value : "", it -> it.getExtensions());
            Labels labels = node.getLabels();
            String identifier = labels.getIdentifier();
            if (identifier != null
                    && labels.getImplied().isEmpty()
                    && labels.getOptional().isEmpty()
                    && labels.getExtensions().getCustom().isEmpty()) {
                node.setLabel(identifier);
                labels.setIdentifier(null);
            }
        }
        for (var relationship : model.getRelationships().values()) {
            Notation.collapse(
                    relationship.getConstraints(),
                    relationship.getProperties(),
                    relationship.getType(),
                    it -> it.getExtensions());
        }
    }

    /**
     * Converts single property constraints into shorthand flags on the property.
     * Constraints with a custom name or extensions are kept, as that data would be lost.
     */
    private static <C extends Constraint & Named> void collapse(
            Map<String, C> constraints,
            Map<String, Property> properties,
            String label,
            Function<C, CustomExtensions> extensions) {
        Set<String> collapsed = new LinkedHashSet<>();
        for (var entry : constraints.entrySet()) {
            var key = entry.getKey();
            var constraint = entry.getValue();
            List<String> constraintProperties = constraint.getProperties();
            String propertyKey = constraintProperties.size() == 1 ? constraintProperties.get(0) : null;
            if (propertyKey == null) {
                continue;
            }
            Property property = properties.get(propertyKey);
            if (property == null) {
                continue;
            }
            String value = property.getName();
            String expected = NameFormat.constraintName(
                    Collections.singletonList(value != null ? value : propertyKey), label, constraint.getType());
            if (!extensions.apply(constraint).getCustom().isEmpty()
                    || constraint.getName() != null && !Objects.equals(constraint.getName(), expected)) {
                continue;
            }
            ConstraintType subject = constraint.getType();
            if (subject == ConstraintType.EXISTS) {
                property.setMustExist(true);
            } else if (subject == ConstraintType.KEY) {
                property.setKey(true);
            } else if (subject == ConstraintType.UNIQUE) {
                property.setUnique(true);
            } else {
                continue;
            }
            collapsed.add(key);
        }
        for (var it : collapsed) {
            constraints.remove(it);
        }
    }

    /**
     * Converts shorthand property constraints into long-hand constraints.
     * Doesn't check or transform (e.g. for overlapping)
     */
    private static <C extends Constraint & Named> void expand(
            Map<String, C> constraints,
            Map<String, Property> properties,
            String entity,
            String label,
            BiFunction<ConstraintType, List<String>, C> constraint) {
        for (var entry : properties.entrySet()) {
            var key = entry.getKey();
            var property = entry.getValue();
            Consumer<ConstraintType> add = type -> {
                var tokenOrNull = property.getName();
                String token = tokenOrNull != null ? tokenOrNull : key;
                C created = constraint.apply(type, new ArrayList<>(Collections.singletonList(key)));
                String name = NameFormat.constraintName(Collections.singletonList(token), label, type);
                if (!name.isBlank()) {
                    created.setName(name);
                }
                constraints.put(Notation.deterministicId(entity, type, token), created);
            };
            if (Objects.equals(property.getKey(), true)) {
                property.setKey(null);
                add.accept(ConstraintType.KEY);
            }
            if (Objects.equals(property.getUnique(), true)) {
                property.setUnique(null);
                add.accept(ConstraintType.UNIQUE);
            }
            if (Objects.equals(property.getMustExist(), true)) {
                property.setMustExist(null);
                add.accept(ConstraintType.EXISTS);
            }
        }
    }

    public static String deterministicId(String entity, ConstraintType type, String... properties) {
        return type.name().toLowerCase(Locale.ROOT) + "_" + entity + "_"
                + Arrays.stream(properties).distinct().sorted().collect(Collectors.joining("_"));
    }
}
