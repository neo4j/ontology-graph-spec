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

/** The target of a foreign key. */
public final class ForeignKeyReference {
    private final String table;
    private final List<String> columns;
    private final Map<String, Object> custom;

    private ForeignKeyReference(Builder b) {
        this.table = b.table;
        this.columns = Collections.unmodifiableList(new ArrayList<>(b.columns));
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

    /** @return id of the referenced table. */
    public String table() {
        return table;
    }

    /** @return names of the referenced columns. */
    public List<String> columns() {
        return columns;
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
        ForeignKeyReference that = (ForeignKeyReference) o;
        return Objects.equals(table, that.table)
                && Objects.equals(columns, that.columns)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, columns, custom);
    }

    @Override
    public String toString() {
        return "ForeignKeyReference{" + "table=" + table + ", columns=" + columns + ", custom=" + custom + "}";
    }

    /** Builder for {@link ForeignKeyReference}. */
    public static final class Builder {
        private String table;
        private final List<String> columns = new ArrayList<>();
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(ForeignKeyReference v) {
            this.table = v.table;
            this.columns.addAll(v.columns);
            this.custom.putAll(v.custom);
        }

        /**
         * Sets id of the referenced table.
         *
         * @param table the value, or null to clear it
         * @return this builder
         */
        public Builder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Replaces names of the referenced columns.
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
        public ForeignKeyReference build() {
            if (table == null) {
                throw new IllegalStateException("ForeignKeyReference: required field 'table' is missing");
            }
            return new ForeignKeyReference(this);
        }
    }
}
