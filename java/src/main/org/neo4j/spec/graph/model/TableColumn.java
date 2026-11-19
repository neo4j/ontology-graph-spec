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

/** A column of a data source table. */
public final class TableColumn {
    private final String name;
    private final String type;
    private final Integer size;
    private final Neo4jType suggested;
    private final List<Neo4jType> supported;
    private final Boolean nullable;
    private final Integer dimension;
    private final Map<String, Object> custom;

    private TableColumn(Builder b) {
        this.name = b.name;
        this.type = b.type;
        this.size = b.size;
        this.suggested = b.suggested;
        this.supported = Collections.unmodifiableList(new ArrayList<>(b.supported));
        this.nullable = b.nullable;
        this.dimension = b.dimension;
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

    /** @return name of the column. */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /** @return source type of the column. */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /** @return size of the column type. */
    public Optional<Integer> size() {
        return Optional.ofNullable(size);
    }

    /** @return suggested Neo4j type for the column. */
    public Optional<Neo4jType> suggested() {
        return Optional.ofNullable(suggested);
    }

    /** @return neo4j types the column can be converted to. */
    public List<Neo4jType> supported() {
        return supported;
    }

    /** @return whether the column can contain nulls. */
    public Optional<Boolean> nullable() {
        return Optional.ofNullable(nullable);
    }

    /** @return dimension of a vector column. */
    public Optional<Integer> dimension() {
        return Optional.ofNullable(dimension);
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
        TableColumn that = (TableColumn) o;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(size, that.size)
                && Objects.equals(suggested, that.suggested)
                && Objects.equals(supported, that.supported)
                && Objects.equals(nullable, that.nullable)
                && Objects.equals(dimension, that.dimension)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, size, suggested, supported, nullable, dimension, custom);
    }

    @Override
    public String toString() {
        return "TableColumn{" + "name="
                + name + ", type="
                + type + ", size="
                + size + ", suggested="
                + suggested + ", supported="
                + supported + ", nullable="
                + nullable + ", dimension="
                + dimension + ", custom="
                + custom + "}";
    }

    /** Builder for {@link TableColumn}. */
    public static final class Builder {
        private String name;
        private String type;
        private Integer size;
        private Neo4jType suggested;
        private final List<Neo4jType> supported = new ArrayList<>();
        private Boolean nullable;
        private Integer dimension;
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(TableColumn v) {
            this.name = v.name;
            this.type = v.type;
            this.size = v.size;
            this.suggested = v.suggested;
            this.supported.addAll(v.supported);
            this.nullable = v.nullable;
            this.dimension = v.dimension;
            this.custom.putAll(v.custom);
        }

        /**
         * Sets name of the column.
         *
         * @param name the value, or null to clear it
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets source type of the column.
         *
         * @param type the value, or null to clear it
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets size of the column type.
         *
         * @param size the value, or null to clear it
         * @return this builder
         */
        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        /**
         * Sets suggested Neo4j type for the column.
         *
         * @param suggested the value, or null to clear it
         * @return this builder
         */
        public Builder suggested(Neo4jType suggested) {
            this.suggested = suggested;
            return this;
        }

        /**
         * Replaces neo4j types the column can be converted to.
         *
         * @param supported the values, or null to clear
         * @return this builder
         */
        public Builder supported(List<Neo4jType> supported) {
            this.supported.clear();
            if (supported != null) {
                this.supported.addAll(supported);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder supportedType(Neo4jType value) {
            this.supported.add(Objects.requireNonNull(value, "supportedType"));
            return this;
        }

        /**
         * Sets whether the column can contain nulls.
         *
         * @param nullable the value, or null to clear it
         * @return this builder
         */
        public Builder nullable(Boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        /**
         * Sets dimension of a vector column.
         *
         * @param dimension the value, or null to clear it
         * @return this builder
         */
        public Builder dimension(Integer dimension) {
            this.dimension = dimension;
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
        public TableColumn build() {
            return new TableColumn(this);
        }
    }
}
