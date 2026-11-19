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

/** A data source table. */
public final class Table {
    private final String source;
    private final List<TableColumn> columns;
    private final List<String> primaryKeys;
    private final Map<String, ForeignKey> foreignKeys;
    private final Map<String, Object> custom;

    private Table(Builder b) {
        this.source = b.source;
        this.columns = Collections.unmodifiableList(new ArrayList<>(b.columns));
        this.primaryKeys = Collections.unmodifiableList(new ArrayList<>(b.primaryKeys));
        this.foreignKeys = Collections.unmodifiableMap(new LinkedHashMap<>(b.foreignKeys));
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

    /** @return id or name of the data source. */
    public String source() {
        return source;
    }

    /** @return columns of the table. */
    public List<TableColumn> columns() {
        return columns;
    }

    /** @return names of the primary key columns (wire key {@code primary_keys}). */
    public List<String> primaryKeys() {
        return primaryKeys;
    }

    /** @return foreign keys keyed by id (wire key {@code foreign_keys}). */
    public Map<String, ForeignKey> foreignKeys() {
        return foreignKeys;
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
        Table that = (Table) o;
        return Objects.equals(source, that.source)
                && Objects.equals(columns, that.columns)
                && Objects.equals(primaryKeys, that.primaryKeys)
                && Objects.equals(foreignKeys, that.foreignKeys)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, columns, primaryKeys, foreignKeys, custom);
    }

    @Override
    public String toString() {
        return "Table{" + "source="
                + source + ", columns="
                + columns + ", primaryKeys="
                + primaryKeys + ", foreignKeys="
                + foreignKeys + ", custom="
                + custom + "}";
    }

    /** Builder for {@link Table}. */
    public static final class Builder {
        private String source;
        private final List<TableColumn> columns = new ArrayList<>();
        private final List<String> primaryKeys = new ArrayList<>();
        private final Map<String, ForeignKey> foreignKeys = new LinkedHashMap<>();
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(Table v) {
            this.source = v.source;
            this.columns.addAll(v.columns);
            this.primaryKeys.addAll(v.primaryKeys);
            this.foreignKeys.putAll(v.foreignKeys);
            this.custom.putAll(v.custom);
        }

        /**
         * Sets id or name of the data source.
         *
         * @param source the value, or null to clear it
         * @return this builder
         */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * Replaces columns of the table.
         *
         * @param columns the values, or null to clear
         * @return this builder
         */
        public Builder columns(List<TableColumn> columns) {
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
        public Builder column(TableColumn value) {
            this.columns.add(Objects.requireNonNull(value, "column"));
            return this;
        }

        /**
         * Replaces names of the primary key columns (wire key {@code primary_keys}).
         *
         * @param primaryKeys the values, or null to clear
         * @return this builder
         */
        public Builder primaryKeys(List<String> primaryKeys) {
            this.primaryKeys.clear();
            if (primaryKeys != null) {
                this.primaryKeys.addAll(primaryKeys);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder primaryKey(String value) {
            this.primaryKeys.add(Objects.requireNonNull(value, "primaryKey"));
            return this;
        }

        /**
         * Replaces foreign keys keyed by id (wire key {@code foreign_keys}).
         *
         * @param foreignKeys the entries, or null to clear
         * @return this builder
         */
        public Builder foreignKeys(Map<String, ForeignKey> foreignKeys) {
            this.foreignKeys.clear();
            if (foreignKeys != null) {
                this.foreignKeys.putAll(foreignKeys);
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
        public Builder foreignKey(String key, ForeignKey value) {
            this.foreignKeys.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
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
        public Table build() {
            if (source == null) {
                throw new IllegalStateException("Table: required field 'source' is missing");
            }
            return new Table(this);
        }
    }
}
