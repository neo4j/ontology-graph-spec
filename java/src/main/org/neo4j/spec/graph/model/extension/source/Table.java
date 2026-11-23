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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

/** A data source table schema. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({"source", "columns", "primary_keys", "foreign_keys", "custom"})
@JsonDeserialize(builder = Table.Builder.class)
public final class Table implements CustomExtensions {
    private String source;

    private final List<TableColumn> columns;

    private final List<String> primaryKeys;

    private final Map<String, ForeignKey> foreignKeys;

    private final Map<String, Object> custom;

    /** Default instance Jackson compares against to omit default values. */
    private Table() {
        this(new Builder(null));
    }

    private Table(Builder builder) {
        this.source = builder.source;
        this.columns = builder.columns;
        this.primaryKeys = builder.primaryKeys;
        this.foreignKeys = builder.foreignKeys;
        this.custom = builder.custom;
    }

    public static Builder builder(String source) {
        return new Builder(source);
    }

    /** Where the table's data comes from. */
    public String getSource() {
        return this.source;
    }

    public void setSource(String value) {
        this.source = value;
    }

    /** Columns of the table in their source order. */
    public List<TableColumn> getColumns() {
        return this.columns;
    }

    /** Names of the columns making up the primary key. */
    @JsonProperty("primary_keys")
    public List<String> getPrimaryKeys() {
        return this.primaryKeys;
    }

    /** Foreign keys of the table keyed by id. */
    @JsonProperty("foreign_keys")
    public Map<String, ForeignKey> getForeignKeys() {
        return this.foreignKeys;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Table)) return false;
        Table that = (Table) other;
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
        return "Table(source=" + source + ", columns=" + columns + ", primaryKeys=" + primaryKeys + ", foreignKeys="
                + foreignKeys + ", custom=" + custom + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String source;
        private List<TableColumn> columns = new ArrayList<>();
        private List<String> primaryKeys = new ArrayList<>();
        private Map<String, ForeignKey> foreignKeys = new LinkedHashMap<>();
        private Map<String, Object> custom = new LinkedHashMap<>();

        @JsonCreator
        private Builder(@JsonProperty("source") String source) {
            this.source = source;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder columns(List<TableColumn> columns) {
            this.columns = columns;
            return this;
        }

        @JsonProperty("primary_keys")
        public Builder primaryKeys(List<String> primaryKeys) {
            this.primaryKeys = primaryKeys;
            return this;
        }

        @JsonProperty("foreign_keys")
        public Builder foreignKeys(Map<String, ForeignKey> foreignKeys) {
            this.foreignKeys = foreignKeys;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public Table build() {
            Objects.requireNonNull(source, "Field 'source' is required");
            return new Table(this);
        }
    }
}
