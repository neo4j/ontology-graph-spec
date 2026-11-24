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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.GraphModelExtensions;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.ValidationTree;

public final class GraphModel {
    private final String schema;

    private final String version;

    private final String id;

    private final String name;

    private final String description;

    private final Map<String, Node> nodes;

    private final Map<String, Relationship> relationships;

    private final GraphModelExtensions extensions;

    private GraphModel(Builder builder) {
        this.schema = builder.schema;
        this.version = builder.version;
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.nodes = builder.nodes;
        this.relationships = builder.relationships;
        this.extensions = builder.extensions;
    }

    public static Builder builder(String version, String id) {
        return new Builder(version, id);
    }

    /** Schema specification uri used. Always encoded, even when left at its default. */
    public String getSchema() {
        return this.schema;
    }

    /** User-defined version of this model document, e.g. a revision number or label. */
    public String getVersion() {
        return this.version;
    }

    /** Unique identifier of the model, typically a kebab-case version of the name, used for referencing. */
    public String getId() {
        return this.id;
    }

    /** Human readable name for the model. */
    public String getName() {
        return this.name;
    }

    /** Description of the model. */
    public String getDescription() {
        return this.description;
    }

    /** Node definitions keyed by node id. */
    public Map<String, Node> getNodes() {
        return this.nodes;
    }

    /** Relationship definitions keyed by relationship id. */
    public Map<String, Relationship> getRelationships() {
        return this.relationships;
    }

    /** Extensions to the model: data source tables, mappings and external custom data. */
    public GraphModelExtensions getExtensions() {
        return this.extensions;
    }

    /**
     * Expands any shorthand in the model into its longhand form, see {@code Notation}.
     */
    public void longhand() {
        Notation.longhand(this);
    }

    /**
     * Collapses longhand in the model into shorthand wherever that loses no information, see {@code Notation}.
     */
    public void shorthand() {
        Notation.shorthand(this);
    }

    public List<Issue> validate(List<? extends Validation> validators) {
        ValidationTree tree = new ValidationTree();
        tree.build(validators);
        return tree.validate(this);
    }

    public static List<Issue> validate(GraphModel model, List<? extends Validation> validators) {
        return model.validate(validators);
    }

    public static void longhand(GraphModel model) {
        model.longhand();
    }

    public static void shorthand(GraphModel model) {
        model.shorthand();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GraphModel)) return false;
        GraphModel that = (GraphModel) other;
        return Objects.equals(schema, that.schema)
                && Objects.equals(version, that.version)
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(nodes, that.nodes)
                && Objects.equals(relationships, that.relationships)
                && Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, version, id, name, description, nodes, relationships, extensions);
    }

    @Override
    public String toString() {
        return "GraphModel(schema=" + schema + ", version=" + version + ", id=" + id + ", name=" + name
                + ", description=" + description + ", nodes=" + nodes + ", relationships=" + relationships
                + ", extensions=" + extensions + ")";
    }

    public static final class Builder {
        private String schema = Version.schemaUrl(Version.LATEST);
        private String version;
        private String id;
        private String name = "";
        private String description = "";
        private Map<String, Node> nodes = new LinkedHashMap<>();
        private Map<String, Relationship> relationships = new LinkedHashMap<>();
        private GraphModelExtensions extensions = GraphModelExtensions.builder().build();

        private Builder(String version, String id) {
            this.version = version;
            this.id = id;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder nodes(Map<String, Node> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder relationships(Map<String, Relationship> relationships) {
            this.relationships = relationships;
            return this;
        }

        public Builder extensions(GraphModelExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        public GraphModel build() {
            Objects.requireNonNull(version, "Field 'version' is required");
            Objects.requireNonNull(id, "Field 'id' is required");
            return new GraphModel(this);
        }
    }
}
