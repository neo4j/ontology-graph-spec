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

/** Extensions for a graph model: data source tables, mappings and custom data. */
public final class GraphModelExtensions {
    private final Map<String, Table> dataSourceTables;
    private final Map<String, List<Mapping>> dataSourceMappings;
    private final Map<String, Object> custom;

    private GraphModelExtensions(Builder b) {
        this.dataSourceTables = Collections.unmodifiableMap(new LinkedHashMap<>(b.dataSourceTables));
        Map<String, List<Mapping>> mappings = new LinkedHashMap<>();
        for (Map.Entry<String, List<Mapping>> e : b.dataSourceMappings.entrySet()) {
            mappings.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        }
        this.dataSourceMappings = Collections.unmodifiableMap(mappings);
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

    /** @return data source table schemas keyed by table id. */
    public Map<String, Table> dataSourceTables() {
        return dataSourceTables;
    }

    /** @return sets of data source mappings from tables to nodes and relationships, keyed by set name. */
    public Map<String, List<Mapping>> dataSourceMappings() {
        return dataSourceMappings;
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
        GraphModelExtensions that = (GraphModelExtensions) o;
        return Objects.equals(dataSourceTables, that.dataSourceTables)
                && Objects.equals(dataSourceMappings, that.dataSourceMappings)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSourceTables, dataSourceMappings, custom);
    }

    @Override
    public String toString() {
        return "GraphModelExtensions{" + "dataSourceTables="
                + dataSourceTables + ", dataSourceMappings="
                + dataSourceMappings + ", custom="
                + custom + "}";
    }

    /** Builder for {@link GraphModelExtensions}. */
    public static final class Builder {
        private final Map<String, Table> dataSourceTables = new LinkedHashMap<>();
        private final Map<String, List<Mapping>> dataSourceMappings = new LinkedHashMap<>();
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(GraphModelExtensions v) {
            this.dataSourceTables.putAll(v.dataSourceTables);
            this.dataSourceMappings.putAll(v.dataSourceMappings);
            this.custom.putAll(v.custom);
        }

        /**
         * Replaces data source table schemas keyed by table id.
         *
         * @param dataSourceTables the entries, or null to clear
         * @return this builder
         */
        public Builder dataSourceTables(Map<String, Table> dataSourceTables) {
            this.dataSourceTables.clear();
            if (dataSourceTables != null) {
                this.dataSourceTables.putAll(dataSourceTables);
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
        public Builder dataSourceTable(String key, Table value) {
            this.dataSourceTables.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Replaces sets of data source mappings from tables to nodes and relationships, keyed by set name.
         *
         * @param dataSourceMappings the entries, or null to clear
         * @return this builder
         */
        public Builder dataSourceMappings(Map<String, List<Mapping>> dataSourceMappings) {
            this.dataSourceMappings.clear();
            if (dataSourceMappings != null) {
                this.dataSourceMappings.putAll(dataSourceMappings);
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
        public Builder dataSourceMapping(String key, List<Mapping> value) {
            this.dataSourceMappings.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
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
        public GraphModelExtensions build() {
            return new GraphModelExtensions(this);
        }
    }
}
