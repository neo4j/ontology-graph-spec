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
package org.neo4j.spec.graph.model.extension.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;
import org.neo4j.spec.graph.model.property.Neo4jType;

/** A column of a data source table. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = TableColumn.Builder.class)
public final class TableColumn implements CustomExtensions {
    private final String name;

    private String type;

    private int size;

    private final Neo4jType suggested;

    private final List<Neo4jType> supported;

    private final boolean nullable;

    private final Integer dimension;

    private final Map<String, Object> custom;

    /** Default instance Jackson compares against to omit default values. */
    private TableColumn() {
        this(new Builder());
    }

    private TableColumn(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.size = builder.size;
        this.suggested = builder.suggested;
        this.supported = builder.supported;
        this.nullable = builder.nullable;
        this.dimension = builder.dimension;
        this.custom = builder.custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Name of the column in the source. Unique within the table. */
    public String getName() {
        return this.name;
    }

    /** Data type of the column in the source. */
    public String getType() {
        return this.type;
    }

    public void setType(String value) {
        this.type = value;
    }

    /** Size of the column. */
    public int getSize() {
        return this.size;
    }

    public void setSize(int value) {
        this.size = value;
    }

    /** Neo4j type suggested for values of this column. */
    public Neo4jType getSuggested() {
        return this.suggested;
    }

    /** Neo4j types values of this column can be converted to when using the import tool. */
    public List<Neo4jType> getSupported() {
        return this.supported;
    }

    /** Whether the column can hold null values. */
    public boolean getNullable() {
        return this.nullable;
    }

    /** Dimension of the column when it holds vectors. */
    public Integer getDimension() {
        return this.dimension;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TableColumn)) return false;
        TableColumn that = (TableColumn) other;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && size == that.size
                && Objects.equals(suggested, that.suggested)
                && Objects.equals(supported, that.supported)
                && nullable == that.nullable
                && Objects.equals(dimension, that.dimension)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, size, suggested, supported, nullable, dimension, custom);
    }

    @Override
    public String toString() {
        return "TableColumn(name=" + name + ", type=" + type + ", size=" + size + ", suggested=" + suggested
                + ", supported=" + supported + ", nullable=" + nullable + ", dimension=" + dimension + ", custom="
                + custom + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String name = "";
        private String type = "";
        private int size = -1;
        private Neo4jType suggested = Neo4jType.ANY;
        private List<Neo4jType> supported = Collections.emptyList();
        private boolean nullable = true;
        private Integer dimension = null;
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder suggested(Neo4jType suggested) {
            this.suggested = suggested;
            return this;
        }

        public Builder supported(List<Neo4jType> supported) {
            this.supported = supported;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder dimension(Integer dimension) {
            this.dimension = dimension;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public TableColumn build() {
            return new TableColumn(this);
        }
    }
}
