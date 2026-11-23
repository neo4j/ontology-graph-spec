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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

@JsonClassDescription("A data source table schema.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class Table implements CustomExtensions {
    private String source;

    private final List<TableColumn> columns;

    private final List<String> primaryKeys;

    private final Map<String, ForeignKey> foreignKeys;

    private final Map<String, Object> custom;

    private Table(
            String source,
            List<TableColumn> columns,
            List<String> primaryKeys,
            Map<String, ForeignKey> foreignKeys,
            Map<String, Object> custom) {
        this.source = source;
        this.columns = columns;
        this.primaryKeys = primaryKeys;
        this.foreignKeys = foreignKeys;
        this.custom = custom;
    }

    public static Builder builder(String source) {
        return new Builder(source);
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String value) {
        this.source = value;
    }

    public List<TableColumn> getColumns() {
        return this.columns;
    }

    public List<String> getPrimaryKeys() {
        return this.primaryKeys;
    }

    public Map<String, ForeignKey> getForeignKeys() {
        return this.foreignKeys;
    }

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
        int result = Objects.hashCode(source);
        result = 31 * result + Objects.hashCode(columns);
        result = 31 * result + Objects.hashCode(primaryKeys);
        result = 31 * result + Objects.hashCode(foreignKeys);
        result = 31 * result + Objects.hashCode(custom);
        return result;
    }

    @Override
    public String toString() {
        return "Table(source=" + source + ", columns=" + columns + ", primaryKeys=" + primaryKeys + ", foreignKeys="
                + foreignKeys + ", custom=" + custom + ")";
    }

    @JsonProperty("source")
    @JsonPropertyDescription("Where the table's data comes from.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonSource() {
        return this.source;
    }

    @JsonProperty("columns")
    @JsonPropertyDescription("Columns of the table in their source order.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<TableColumn> jsonColumns() {
        return Objects.equals(this.columns, new ArrayList<TableColumn>()) ? null : this.columns;
    }

    @JsonProperty("primary_keys")
    @JsonPropertyDescription("Names of the columns making up the primary key.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonPrimaryKeys() {
        return Objects.equals(this.primaryKeys, new ArrayList<String>()) ? null : this.primaryKeys;
    }

    @JsonProperty("foreign_keys")
    @JsonPropertyDescription("Foreign keys of the table keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, ForeignKey> jsonForeignKeys() {
        return Objects.equals(this.foreignKeys, new LinkedHashMap<String, ForeignKey>()) ? null : this.foreignKeys;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static Table fromJson(
            @JsonProperty("source") String source,
            @JsonProperty("columns") List<TableColumn> columns,
            @JsonProperty("primary_keys") List<String> primaryKeys,
            @JsonProperty("foreign_keys") Map<String, ForeignKey> foreignKeys,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new Table(
                Objects.requireNonNull(source, "Field 'source' is required"),
                columns != null ? columns : new ArrayList<TableColumn>(),
                primaryKeys != null ? primaryKeys : new ArrayList<String>(),
                foreignKeys != null ? foreignKeys : new LinkedHashMap<String, ForeignKey>(),
                custom != null ? custom : new LinkedHashMap<String, Object>());
    }

    public static final class Builder {
        private String source;
        private List<TableColumn> columns = new ArrayList<TableColumn>();
        private List<String> primaryKeys = new ArrayList<String>();
        private Map<String, ForeignKey> foreignKeys = new LinkedHashMap<String, ForeignKey>();
        private Map<String, Object> custom = new LinkedHashMap<String, Object>();

        private Builder(String source) {
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

        public Builder primaryKeys(List<String> primaryKeys) {
            this.primaryKeys = primaryKeys;
            return this;
        }

        public Builder foreignKeys(Map<String, ForeignKey> foreignKeys) {
            this.foreignKeys = foreignKeys;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public Table build() {
            return new Table(source, columns, primaryKeys, foreignKeys, custom);
        }
    }
}
