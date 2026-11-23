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

@JsonClassDescription("The target of a foreign key.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class ForeignKeyReference implements CustomExtensions {
    private String table;

    private final List<String> columns;

    private final Map<String, Object> custom;

    private ForeignKeyReference(String table, List<String> columns, Map<String, Object> custom) {
        this.table = table;
        this.columns = columns;
        this.custom = custom;
    }

    public static Builder builder(String table) {
        return new Builder(table);
    }

    public String getTable() {
        return this.table;
    }

    public void setTable(String value) {
        this.table = value;
    }

    public List<String> getColumns() {
        return this.columns;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ForeignKeyReference)) return false;
        ForeignKeyReference that = (ForeignKeyReference) other;
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
        return "ForeignKeyReference(table=" + table + ", columns=" + columns + ", custom=" + custom + ")";
    }

    @JsonProperty("table")
    @JsonPropertyDescription("Id of the referenced table.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonTable() {
        return this.table;
    }

    @JsonProperty("columns")
    @JsonPropertyDescription("Names of the referenced columns.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonColumns() {
        return Objects.equals(this.columns, new ArrayList<String>()) ? null : this.columns;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static ForeignKeyReference fromJson(
            @JsonProperty("table") String table,
            @JsonProperty("columns") List<String> columns,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new ForeignKeyReference(
                Objects.requireNonNull(table, "Field 'table' is required"),
                columns != null ? columns : new ArrayList<>(),
                custom != null ? custom : new LinkedHashMap<>());
    }

    public static final class Builder {
        private String table;
        private List<String> columns = new ArrayList<>();
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder(String table) {
            this.table = table;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder columns(List<String> columns) {
            this.columns = columns;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public ForeignKeyReference build() {
            return new ForeignKeyReference(table, columns, custom);
        }
    }
}
