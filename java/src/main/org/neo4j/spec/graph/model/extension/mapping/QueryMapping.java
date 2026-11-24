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

import java.util.Objects;

/** Maps the results of a query run against a table. */
public final class QueryMapping implements Mapping {
    private String table;

    private String query;

    private String dependsOn;

    private String id;

    private QueryMapping(Builder builder) {
        this.table = builder.table;
        this.query = builder.query;
        this.dependsOn = builder.dependsOn;
        this.id = builder.id;
    }

    public static Builder builder(String table, String query) {
        return new Builder(table, query);
    }

    /** Id of the source table. */
    public String getTable() {
        return this.table;
    }

    public void setTable(String value) {
        this.table = value;
    }

    /** The query to run. */
    public String getQuery() {
        return this.query;
    }

    public void setQuery(String value) {
        this.query = value;
    }

    /** Id of another mapping in the same list which must be imported before this one. Empty when there is no dependency. */
    public String getDependsOn() {
        return this.dependsOn;
    }

    public void setDependsOn(String value) {
        this.dependsOn = value;
    }

    /** Optional id of this mapping, so other mappings in the same list can reference it in depends_on. */
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
        return Objects.hash(table, query, dependsOn, id);
    }

    @Override
    public String toString() {
        return "QueryMapping(table=" + table + ", query=" + query + ", dependsOn=" + dependsOn + ", id=" + id + ")";
    }

    public static final class Builder {
        private String table;
        private String query;
        private String dependsOn = "";
        private String id = "";

        private Builder(String table, String query) {
            this.table = table;
            this.query = query;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder dependsOn(String dependsOn) {
            this.dependsOn = dependsOn;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public QueryMapping build() {
            Objects.requireNonNull(table, "Field 'table' is required");
            Objects.requireNonNull(query, "Field 'query' is required");
            return new QueryMapping(this);
        }
    }
}
