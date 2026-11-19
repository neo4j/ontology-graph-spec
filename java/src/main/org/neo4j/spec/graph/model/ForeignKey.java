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

/** A foreign key of a table. */
public final class ForeignKey {
    private final List<String> columns;
    private final ForeignKeyReference references;
    private final Map<String, Object> custom;

    private ForeignKey(Builder b) {
        this.columns = Collections.unmodifiableList(new ArrayList<>(b.columns));
        this.references = b.references;
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

    /** @return names of the referencing columns. */
    public List<String> columns() {
        return columns;
    }

    /** @return the referenced table and columns. */
    public ForeignKeyReference references() {
        return references;
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
        ForeignKey that = (ForeignKey) o;
        return Objects.equals(columns, that.columns)
                && Objects.equals(references, that.references)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, references, custom);
    }

    @Override
    public String toString() {
        return "ForeignKey{" + "columns=" + columns + ", references=" + references + ", custom=" + custom + "}";
    }

    /** Builder for {@link ForeignKey}. */
    public static final class Builder {
        private final List<String> columns = new ArrayList<>();
        private ForeignKeyReference references;
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(ForeignKey v) {
            this.columns.addAll(v.columns);
            this.references = v.references;
            this.custom.putAll(v.custom);
        }

        /**
         * Replaces names of the referencing columns.
         *
         * @param columns the values, or null to clear
         * @return this builder
         */
        public Builder columns(List<String> columns) {
            this.columns.clear();
            if (columns != null) {
                this.columns.addAll(columns);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder column(String value) {
            this.columns.add(Objects.requireNonNull(value, "column"));
            return this;
        }

        /**
         * Sets the referenced table and columns.
         *
         * @param references the value, or null to clear it
         * @return this builder
         */
        public Builder references(ForeignKeyReference references) {
            this.references = references;
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
        public ForeignKey build() {
            if (references == null) {
                throw new IllegalStateException("ForeignKey: required field 'references' is missing");
            }
            return new ForeignKey(this);
        }
    }
}
