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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Maps rows of a table to nodes. */
public final class NodeMapping extends Mapping {
    private final String node;
    private final String table;
    private final Map<String, PropertyMapping> properties;
    private final MappingMode mode;
    private final String matchLabel;
    private final List<String> key;
    private final String dependsOn;
    private final String id;

    private NodeMapping(Builder b) {
        this.node = b.node;
        this.table = b.table;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(b.properties));
        this.mode = b.mode;
        this.matchLabel = b.matchLabel;
        this.key = Collections.unmodifiableList(new ArrayList<>(b.key));
        this.dependsOn = b.dependsOn;
        this.id = b.id;
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

    /** @return id of the node being mapped. */
    public String node() {
        return node;
    }

    /** @return id of the source table. */
    public String table() {
        return table;
    }

    /** @return mappings from property ids to source columns. */
    public Map<String, PropertyMapping> properties() {
        return properties;
    }

    /** @return whether nodes are merged with existing nodes or always created. */
    public Optional<MappingMode> mode() {
        return Optional.ofNullable(mode);
    }

    /** @return label used to match existing nodes (wire key {@code match_label}). */
    public Optional<String> matchLabel() {
        return Optional.ofNullable(matchLabel);
    }

    /** @return ids of the properties which identify a node. */
    public List<String> key() {
        return key;
    }

    /** @return id of another mapping in the same list which must be imported before this one (wire key {@code depends_on}). */
    public Optional<String> dependsOn() {
        return Optional.ofNullable(dependsOn);
    }

    /** @return optional id of this mapping, so other mappings in the same list can reference it in depends_on. */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    @Override
    public MappingType type() {
        return MappingType.NODE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeMapping that = (NodeMapping) o;
        return Objects.equals(node, that.node)
                && Objects.equals(table, that.table)
                && Objects.equals(properties, that.properties)
                && Objects.equals(mode, that.mode)
                && Objects.equals(matchLabel, that.matchLabel)
                && Objects.equals(key, that.key)
                && Objects.equals(dependsOn, that.dependsOn)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, table, properties, mode, matchLabel, key, dependsOn, id);
    }

    @Override
    public String toString() {
        return "NodeMapping{" + "node="
                + node + ", table="
                + table + ", properties="
                + properties + ", mode="
                + mode + ", matchLabel="
                + matchLabel + ", key="
                + key + ", dependsOn="
                + dependsOn + ", id="
                + id + "}";
    }

    /** Builder for {@link NodeMapping}. */
    public static final class Builder {
        private String node;
        private String table;
        private final Map<String, PropertyMapping> properties = new LinkedHashMap<>();
        private MappingMode mode;
        private String matchLabel;
        private final List<String> key = new ArrayList<>();
        private String dependsOn;
        private String id;

        private Builder() {}

        private Builder(NodeMapping v) {
            this.node = v.node;
            this.table = v.table;
            this.properties.putAll(v.properties);
            this.mode = v.mode;
            this.matchLabel = v.matchLabel;
            this.key.addAll(v.key);
            this.dependsOn = v.dependsOn;
            this.id = v.id;
        }

        /**
         * Sets id of the node being mapped.
         *
         * @param node the value, or null to clear it
         * @return this builder
         */
        public Builder node(String node) {
            this.node = node;
            return this;
        }

        /**
         * Sets id of the source table.
         *
         * @param table the value, or null to clear it
         * @return this builder
         */
        public Builder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Replaces mappings from property ids to source columns.
         *
         * @param properties the entries, or null to clear
         * @return this builder
         */
        public Builder properties(Map<String, PropertyMapping> properties) {
            this.properties.clear();
            if (properties != null) {
                this.properties.putAll(properties);
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
        public Builder property(String key, PropertyMapping value) {
            this.properties.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Sets whether nodes are merged with existing nodes or always created.
         *
         * @param mode the value, or null to clear it
         * @return this builder
         */
        public Builder mode(MappingMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Sets label used to match existing nodes (wire key {@code match_label}).
         *
         * @param matchLabel the value, or null to clear it
         * @return this builder
         */
        public Builder matchLabel(String matchLabel) {
            this.matchLabel = matchLabel;
            return this;
        }

        /**
         * Replaces ids of the properties which identify a node.
         *
         * @param key the values, or null to clear
         * @return this builder
         */
        public Builder key(List<String> key) {
            this.key.clear();
            if (key != null) {
                this.key.addAll(key);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder keyProperty(String value) {
            this.key.add(Objects.requireNonNull(value, "keyProperty"));
            return this;
        }

        /**
         * Sets id of another mapping in the same list which must be imported before this one (wire key {@code depends_on}).
         *
         * @param dependsOn the value, or null to clear it
         * @return this builder
         */
        public Builder dependsOn(String dependsOn) {
            this.dependsOn = dependsOn;
            return this;
        }

        /**
         * Sets optional id of this mapping, so other mappings in the same list can reference it in depends_on.
         *
         * @param id the value, or null to clear it
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public NodeMapping build() {
            if (node == null) {
                throw new IllegalStateException("NodeMapping: required field 'node' is missing");
            }
            if (table == null) {
                throw new IllegalStateException("NodeMapping: required field 'table' is missing");
            }
            return new NodeMapping(this);
        }
    }
}
