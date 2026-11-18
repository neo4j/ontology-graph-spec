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
package org.neo4j.spec.graph.model.extension.mapping;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;

@JsonClassDescription("Maps the results of a query run against a table.")
@JsonTypeName("QUERY")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class QueryMapping implements Mapping {
    private String table;

    private String query;

    private String dependsOn;

    private String id;

    public QueryMapping(String table, String query, String dependsOn, String id) {
        this.table = table;
        this.query = query;
        this.dependsOn = dependsOn;
        this.id = id;
    }

    public QueryMapping(String table, String query, String dependsOn) {
        this(table, query, dependsOn, "");
    }

    public QueryMapping(String table, String query) {
        this(table, query, "", "");
    }

    public String getTable() {
        return this.table;
    }

    public void setTable(String value) {
        this.table = value;
    }

    public String getQuery() {
        return this.query;
    }

    public void setQuery(String value) {
        this.query = value;
    }

    public String getDependsOn() {
        return this.dependsOn;
    }

    public void setDependsOn(String value) {
        this.dependsOn = value;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String value) {
        this.id = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof QueryMapping)) return false;
        QueryMapping that = (QueryMapping) other;
        return Objects.equals(table, that.table)
                && Objects.equals(query, that.query)
                && Objects.equals(dependsOn, that.dependsOn)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(table);
        result = 31 * result + Objects.hashCode(query);
        result = 31 * result + Objects.hashCode(dependsOn);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }

    @Override
    public String toString() {
        return "QueryMapping(table=" + table + ", query=" + query + ", dependsOn=" + dependsOn + ", id=" + id + ")";
    }

    @JsonProperty("table")
    @JsonPropertyDescription("Id of the source table.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonTable() {
        return this.table;
    }

    @JsonProperty("query")
    @JsonPropertyDescription("The query to run.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonQuery() {
        return this.query;
    }

    @JsonProperty("depends_on")
    @JsonPropertyDescription(
            "Id of another mapping in the same list which must be imported before this one. Empty when there is no dependency.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonDependsOn() {
        return Objects.equals(this.dependsOn, "") ? null : this.dependsOn;
    }

    @JsonProperty("id")
    @JsonPropertyDescription(
            "Optional id of this mapping, so other mappings in the same list can reference it in depends_on.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonId() {
        return Objects.equals(this.id, "") ? null : this.id;
    }

    @JsonCreator
    private static QueryMapping fromJson(
            @JsonProperty("table") String table,
            @JsonProperty("query") String query,
            @JsonProperty("depends_on") String dependsOn,
            @JsonProperty("id") String id) {
        return new QueryMapping(
                Objects.requireNonNull(table, "Field 'table' is required"),
                Objects.requireNonNull(query, "Field 'query' is required"),
                dependsOn != null ? dependsOn : "",
                id != null ? id : "");
    }
}
