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

/** Extensions for a node. */
public final class NodeExtensions {
    private final Map<String, NodeIndex> indexes;
    private final NodeDisplay display;
    private final Map<String, Object> custom;

    private NodeExtensions(Builder b) {
        this.indexes = Collections.unmodifiableMap(new LinkedHashMap<>(b.indexes));
        this.display = b.display;
        this.custom = Values.deepUnmodifiable(b.custom);
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

    /** @return indexes on the node keyed by id. */
    public Map<String, NodeIndex> indexes() {
        return indexes;
    }

    /** @return visual information for the node. */
    public Optional<NodeDisplay> display() {
        return Optional.ofNullable(display);
    }

    /** @return user-defined arbitrary data. */
    public Map<String, Object> custom() {
        return custom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeExtensions that = (NodeExtensions) o;
        return Objects.equals(indexes, that.indexes)
                && Objects.equals(display, that.display)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes, display, custom);
    }

    @Override
    public String toString() {
        return "NodeExtensions{" + "indexes=" + indexes + ", display=" + display + ", custom=" + custom + "}";
    }

    /** Builder for {@link NodeExtensions}. */
    public static final class Builder {
        private final Map<String, NodeIndex> indexes = new LinkedHashMap<>();
        private NodeDisplay display;
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(NodeExtensions v) {
            this.indexes.putAll(v.indexes);
            this.display = v.display;
            this.custom.putAll(v.custom);
        }

        /**
         * Replaces indexes on the node keyed by id.
         *
         * @param indexes the entries, or null to clear
         * @return this builder
         */
        public Builder indexes(Map<String, NodeIndex> indexes) {
            this.indexes.clear();
            if (indexes != null) {
                this.indexes.putAll(indexes);
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
        public Builder index(String key, NodeIndex value) {
            this.indexes.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Sets visual information for the node.
         *
         * @param display the value, or null to clear it
         * @return this builder
         */
        public Builder display(NodeDisplay display) {
            this.display = display;
            return this;
        }

        /**
         * Replaces user-defined arbitrary data.
         *
         * @param custom the entries, or null to clear
         * @return this builder
         */
        public Builder custom(Map<String, Object> custom) {
            this.custom.clear();
            if (custom != null) {
                this.custom.putAll(custom);
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
        public Builder custom(String key, Object value) {
            this.custom.put(Objects.requireNonNull(key, "key"), value);
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public NodeExtensions build() {
            return new NodeExtensions(this);
        }
    }
}
