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

/** Extensions for a relationship. */
public final class RelationshipExtensions {
    private final Map<String, RelationshipIndex> indexes;
    private final Map<String, Object> custom;

    private RelationshipExtensions(Builder b) {
        this.indexes = Collections.unmodifiableMap(new LinkedHashMap<>(b.indexes));
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

    /** @return indexes on the relationship keyed by id. */
    public Map<String, RelationshipIndex> indexes() {
        return indexes;
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
        RelationshipExtensions that = (RelationshipExtensions) o;
        return Objects.equals(indexes, that.indexes) && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes, custom);
    }

    @Override
    public String toString() {
        return "RelationshipExtensions{" + "indexes=" + indexes + ", custom=" + custom + "}";
    }

    /** Builder for {@link RelationshipExtensions}. */
    public static final class Builder {
        private final Map<String, RelationshipIndex> indexes = new LinkedHashMap<>();
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(RelationshipExtensions v) {
            this.indexes.putAll(v.indexes);
            this.custom.putAll(v.custom);
        }

        /**
         * Replaces indexes on the relationship keyed by id.
         *
         * @param indexes the entries, or null to clear
         * @return this builder
         */
        public Builder indexes(Map<String, RelationshipIndex> indexes) {
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
        public Builder index(String key, RelationshipIndex value) {
            this.indexes.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
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
        public RelationshipExtensions build() {
            return new RelationshipExtensions(this);
        }
    }
}
