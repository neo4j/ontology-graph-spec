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

import java.util.Objects;
import java.util.Optional;

/** Maps the results of a query run against a table. */
public final class QueryMapping extends Mapping {
    private final String table;
    private final String query;
    private final String dependsOn;
    private final String id;

    private QueryMapping(Builder b) {
        this.table = b.table;
        this.query = b.query;
        this.dependsOn = b.dependsOn;
        this.id = b.id;
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

    /** @return id of the source table. */
    public String table() {
        return table;
    }

    /** @return the query to run. */
    public String query() {
        return query;
    }

    /** @return id of another mapping in the same list which must be imported before this one (wire key {@code depends_on}). */
    public Optional<String> dependsOn() {
        return Optional.ofNullable(dependsOn);
    }

    /** @return optional id of this mapping, so other mappings in the same list can reference it in depends_on. */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    @Override
    public MappingType type() {
        return MappingType.QUERY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryMapping that = (QueryMapping) o;
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
        return "QueryMapping{" + "table="
                + table + ", query="
                + query + ", dependsOn="
                + dependsOn + ", id="
                + id + "}";
    }

    /** Builder for {@link QueryMapping}. */
    public static final class Builder {
        private String table;
        private String query;
        private String dependsOn;
        private String id;

        private Builder() {}

        private Builder(QueryMapping v) {
            this.table = v.table;
            this.query = v.query;
            this.dependsOn = v.dependsOn;
            this.id = v.id;
        }

        /**
         * Sets id of the source table.
         *
         * @param table the value, or null to clear it
         * @return this builder
         */
        public Builder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Sets the query to run.
         *
         * @param query the value, or null to clear it
         * @return this builder
         */
        public Builder query(String query) {
            this.query = query;
            return this;
        }

        /**
         * Sets id of another mapping in the same list which must be imported before this one (wire key {@code depends_on}).
         *
         * @param dependsOn the value, or null to clear it
         * @return this builder
         */
        public Builder dependsOn(String dependsOn) {
            this.dependsOn = dependsOn;
            return this;
        }

        /**
         * Sets optional id of this mapping, so other mappings in the same list can reference it in depends_on.
         *
         * @param id the value, or null to clear it
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public QueryMapping build() {
            if (table == null) {
                throw new IllegalStateException("QueryMapping: required field 'table' is missing");
            }
            if (query == null) {
                throw new IllegalStateException("QueryMapping: required field 'query' is missing");
            }
            return new QueryMapping(this);
        }
    }
}
