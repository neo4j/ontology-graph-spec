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
package org.neo4j.spec.graph.internal.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.neo4j.spec.graph.model.ConstraintType;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.IndexOptions;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.model.RelationshipIndex;

/**
 * A flat view of the constraints and indexes of nodes and relationships, which the schema validators share. Internal:
 * not part of the supported API.
 */
public final class Schema {

    private Schema() {}

    /** The identifier label of a node, if it has a non-blank one. */
    public static Optional<String> identifier(Node node) {
        return node.labels().flatMap(labels -> labels.identifier()).filter(label -> !isBlank(label));
    }

    /** Every label of a node: its identifier, implied and optional labels. */
    public static Set<String> labels(Node node) {
        Set<String> labels = new LinkedHashSet<>();
        node.labels().ifPresent(l -> {
            l.identifier().ifPresent(labels::add);
            labels.addAll(l.implied());
            labels.addAll(l.optional());
        });
        return labels;
    }

    /** Whether text is absent or only whitespace. */
    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /** Every constraint and index, in document order: each node's constraints then indexes, then relationships'. */
    public static List<Element> elements(GraphModel model) {
        List<Element> elements = new ArrayList<>();
        for (Map.Entry<String, Node> entry : model.nodes().entrySet()) {
            String id = entry.getKey();
            Node node = entry.getValue();
            String path = "nodes." + id;
            String target = identifier(node).map(label -> "label:" + label).orElse("node:" + id);
            for (Map.Entry<String, NodeConstraint> c : node.constraints().entrySet()) {
                NodeConstraint constraint = c.getValue();
                elements.add(new Constraint(
                        true,
                        id,
                        c.getKey(),
                        path + ".constraints." + c.getKey(),
                        target,
                        constraint.name().orElse(null),
                        constraint.properties(),
                        node.properties(),
                        constraint.type()));
            }
            node.extensions().ifPresent(extensions -> {
                for (Map.Entry<String, NodeIndex> i : extensions.indexes().entrySet()) {
                    NodeIndex index = i.getValue();
                    elements.add(new Index(
                            true,
                            id,
                            i.getKey(),
                            path + ".extensions.indexes." + i.getKey(),
                            target,
                            index.name().orElse(null),
                            index.properties(),
                            node.properties(),
                            index.type(),
                            index.labels(),
                            labels(node),
                            index.options().orElse(null)));
                }
            });
        }
        for (Map.Entry<String, Relationship> entry : model.relationships().entrySet()) {
            String id = entry.getKey();
            Relationship relationship = entry.getValue();
            String path = "relationships." + id;
            String target = isBlank(relationship.type()) ? "relationship:" + id : "type:" + relationship.type();
            for (Map.Entry<String, RelationshipConstraint> c :
                    relationship.constraints().entrySet()) {
                RelationshipConstraint constraint = c.getValue();
                elements.add(new Constraint(
                        false,
                        id,
                        c.getKey(),
                        path + ".constraints." + c.getKey(),
                        target,
                        constraint.name().orElse(null),
                        constraint.properties(),
                        relationship.properties(),
                        constraint.type()));
            }
            relationship.extensions().ifPresent(extensions -> {
                for (Map.Entry<String, RelationshipIndex> i :
                        extensions.indexes().entrySet()) {
                    RelationshipIndex index = i.getValue();
                    elements.add(new Index(
                            false,
                            id,
                            i.getKey(),
                            path + ".extensions.indexes." + i.getKey(),
                            target,
                            index.name().orElse(null),
                            index.properties(),
                            relationship.properties(),
                            index.type(),
                            List.of(),
                            Set.of(),
                            index.options().orElse(null)));
                }
            });
        }
        return Collections.unmodifiableList(elements);
    }

    /** The constraints of {@link #elements(GraphModel)}, in the same order. */
    public static List<Constraint> constraints(GraphModel model) {
        List<Constraint> constraints = new ArrayList<>();
        for (Element element : elements(model)) {
            if (element instanceof Constraint) {
                constraints.add((Constraint) element);
            }
        }
        return constraints;
    }

    /** The indexes of {@link #elements(GraphModel)}, in the same order. */
    public static List<Index> indexes(GraphModel model) {
        List<Index> indexes = new ArrayList<>();
        for (Element element : elements(model)) {
            if (element instanceof Index) {
                indexes.add((Index) element);
            }
        }
        return indexes;
    }

    /** A constraint or an index of a node or relationship. */
    public abstract static class Element {

        private final boolean node;
        private final String ownerId;
        private final String id;
        private final String path;
        private final String target;
        private final String name;
        private final List<String> properties;
        private final Map<String, Property> available;

        Element(
                boolean node,
                String ownerId,
                String id,
                String path,
                String target,
                String name,
                List<String> properties,
                Map<String, Property> available) {
            this.node = node;
            this.ownerId = ownerId;
            this.id = id;
            this.path = path;
            this.target = target;
            this.name = name;
            this.properties = properties;
            this.available = available;
        }

        /** Whether the owner is a node, rather than a relationship. */
        public boolean isNode() {
            return node;
        }

        /** {@code node} or {@code relationship}, as used in issue codes. */
        public String owner() {
            return node ? "node" : "relationship";
        }

        /** The id of the node or relationship. */
        public String ownerId() {
            return ownerId;
        }

        /** The id of the constraint or index. */
        public String id() {
            return id;
        }

        /** The path of the constraint or index. */
        public String path() {
            return path;
        }

        /**
         * What the constraint or index applies to in the database: the identifier label of a node (or its id when it
         * has none), or the type of a relationship. Distinct for nodes and relationships.
         */
        public String target() {
            return target;
        }

        /** The name, if set and not blank. */
        public Optional<String> name() {
            return Optional.ofNullable(name).filter(n -> !isBlank(n));
        }

        /** The ids of the properties covered, in order. */
        public List<String> properties() {
            return properties;
        }

        /** The properties of the owner, which may not include every one in {@link #properties()}. */
        public Map<String, Property> available() {
            return available;
        }
    }

    /** A constraint of a node or relationship. */
    public static final class Constraint extends Element {

        private final ConstraintType type;

        Constraint(
                boolean node,
                String ownerId,
                String id,
                String path,
                String target,
                String name,
                List<String> properties,
                Map<String, Property> available,
                ConstraintType type) {
            super(node, ownerId, id, path, target, name, properties, available);
            this.type = type;
        }

        /** The type of the constraint. */
        public ConstraintType type() {
            return type;
        }
    }

    /** An index of a node or relationship. */
    public static final class Index extends Element {

        private final IndexType type;
        private final List<String> labels;
        private final Set<String> ownerLabels;
        private final IndexOptions options;

        Index(
                boolean node,
                String ownerId,
                String id,
                String path,
                String target,
                String name,
                List<String> properties,
                Map<String, Property> available,
                IndexType type,
                List<String> labels,
                Set<String> ownerLabels,
                IndexOptions options) {
            super(node, ownerId, id, path, target, name, properties, available);
            this.type = type;
            this.labels = labels;
            this.ownerLabels = ownerLabels;
            this.options = options;
        }

        /** The type of the index. */
        public IndexType type() {
            return type;
        }

        /** The labels indexed, in order. Empty for relationships. */
        public List<String> labels() {
            return labels;
        }

        /** Every label of the node the index is on, which is empty for relationships. */
        public Set<String> ownerLabels() {
            return ownerLabels;
        }

        /** The options of the index, if set. */
        public Optional<IndexOptions> options() {
            return Optional.ofNullable(options);
        }
    }
}
