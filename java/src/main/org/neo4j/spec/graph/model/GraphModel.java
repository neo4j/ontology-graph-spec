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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A graph model document, always in the latest specification version and in longhand. */
public final class GraphModel {
    private final String schema;
    private final String version;
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, Node> nodes;
    private final Map<String, Relationship> relationships;
    private final GraphModelExtensions extensions;

    private GraphModel(Builder b) {
        this.schema = b.schema;
        this.version = b.version;
        this.id = b.id;
        this.name = b.name;
        this.description = b.description;
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(b.nodes));
        this.relationships = Collections.unmodifiableMap(new LinkedHashMap<>(b.relationships));
        this.extensions = b.extensions;
    }

    /**
     * Creates an empty builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialised with the values of this instance.
     *
     * @return a new builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** @return schema specification uri used. */
    public String schema() {
        return schema;
    }

    /** @return user-defined version of this model document, e.g. a revision number or label. */
    public String version() {
        return version;
    }

    /** @return unique identifier of the model, typically a kebab-case version of the name. */
    public String id() {
        return id;
    }

    /** @return human readable name for the model. */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /** @return description of the model. */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /** @return node definitions keyed by node id. */
    public Map<String, Node> nodes() {
        return nodes;
    }

    /** @return relationship definitions keyed by relationship id. */
    public Map<String, Relationship> relationships() {
        return relationships;
    }

    /** @return extensions to the model: data source tables, mappings and external custom data. */
    public Optional<GraphModelExtensions> extensions() {
        return Optional.ofNullable(extensions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GraphModel that = (GraphModel) o;
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
        return "GraphModel{" + "schema="
                + schema + ", version="
                + version + ", id="
                + id + ", name="
                + name + ", description="
                + description + ", nodes="
                + nodes + ", relationships="
                + relationships + ", extensions="
                + extensions + "}";
    }

    /** Builder for {@link GraphModel}. */
    public static final class Builder {
        private String schema;
        private String version;
        private String id;
        private String name;
        private String description;
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private final Map<String, Relationship> relationships = new LinkedHashMap<>();
        private GraphModelExtensions extensions;

        private Builder() {}

        private Builder(GraphModel v) {
            this.schema = v.schema;
            this.version = v.version;
            this.id = v.id;
            this.name = v.name;
            this.description = v.description;
            this.nodes.putAll(v.nodes);
            this.relationships.putAll(v.relationships);
            this.extensions = v.extensions;
        }

        /**
         * Sets schema specification uri used.
         *
         * @param schema the value, or null to clear it
         * @return this builder
         */
        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Sets user-defined version of this model document, e.g. a revision number or label.
         *
         * @param version the value, or null to clear it
         * @return this builder
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets unique identifier of the model, typically a kebab-case version of the name.
         *
         * @param id the value, or null to clear it
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets human readable name for the model.
         *
         * @param name the value, or null to clear it
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets description of the model.
         *
         * @param description the value, or null to clear it
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Replaces node definitions keyed by node id.
         *
         * @param nodes the entries, or null to clear
         * @return this builder
         */
        public Builder nodes(Map<String, Node> nodes) {
            this.nodes.clear();
            if (nodes != null) {
                this.nodes.putAll(nodes);
            }
            return this;
        }

        /**
         * Adds or replaces one entry.
         *
         * @param key the entry key
         * @param value the entry value
         * @return this builder
         */
        public Builder node(String key, Node value) {
            this.nodes.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Replaces relationship definitions keyed by relationship id.
         *
         * @param relationships the entries, or null to clear
         * @return this builder
         */
        public Builder relationships(Map<String, Relationship> relationships) {
            this.relationships.clear();
            if (relationships != null) {
                this.relationships.putAll(relationships);
            }
            return this;
        }

        /**
         * Adds or replaces one entry.
         *
         * @param key the entry key
         * @param value the entry value
         * @return this builder
         */
        public Builder relationship(String key, Relationship value) {
            this.relationships.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Sets extensions to the model: data source tables, mappings and external custom data.
         *
         * @param extensions the value, or null to clear it
         * @return this builder
         */
        public Builder extensions(GraphModelExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public GraphModel build() {
            if (schema == null) {
                throw new IllegalStateException("GraphModel: required field 'schema' is missing");
            }
            if (version == null) {
                throw new IllegalStateException("GraphModel: required field 'version' is missing");
            }
            if (id == null) {
                throw new IllegalStateException("GraphModel: required field 'id' is missing");
            }
            return new GraphModel(this);
        }
    }
}
