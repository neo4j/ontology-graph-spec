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

/** Identifies a node at one end of a mapped relationship. */
public final class TargetMapping {
    private final String label;
    private final Map<String, PropertyMapping> properties;

    private TargetMapping(Builder b) {
        this.label = b.label;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(b.properties));
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

    /** @return label of the node. */
    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    /** @return mappings from property ids to source columns used to match the node. */
    public Map<String, PropertyMapping> properties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TargetMapping that = (TargetMapping) o;
        return Objects.equals(label, that.label) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, properties);
    }

    @Override
    public String toString() {
        return "TargetMapping{" + "label=" + label + ", properties=" + properties + "}";
    }

    /** Builder for {@link TargetMapping}. */
    public static final class Builder {
        private String label;
        private final Map<String, PropertyMapping> properties = new LinkedHashMap<>();

        private Builder() {}

        private Builder(TargetMapping v) {
            this.label = v.label;
            this.properties.putAll(v.properties);
        }

        /**
         * Sets label of the node.
         *
         * @param label the value, or null to clear it
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Replaces mappings from property ids to source columns used to match the node.
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
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public TargetMapping build() {
            return new TargetMapping(this);
        }
    }
}
