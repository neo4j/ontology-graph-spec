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
package org.neo4j.spec.graph.model.extension;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.mapping.Mapping;
import org.neo4j.spec.graph.model.extension.source.Table;

/**
 * Extensions for a {@code GraphModel}.
 */
/** Extensions for a graph model. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({"data_source_tables", "data_source_mappings", "custom"})
@JsonDeserialize(builder = GraphModelExtensions.Builder.class)
public final class GraphModelExtensions implements CustomExtensions {
    private final Map<String, Table> dataSourceTables;

    private final Map<String, List<Mapping>> dataSourceMappings;

    private final Map<String, Object> custom;

    /** Default instance Jackson compares against to omit default values. */
    private GraphModelExtensions() {
        this(new Builder());
    }

    private GraphModelExtensions(Builder builder) {
        this.dataSourceTables = builder.dataSourceTables;
        this.dataSourceMappings = builder.dataSourceMappings;
        this.custom = builder.custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Data source table schemas keyed by table id. */
    @JsonProperty("data_source_tables")
    public Map<String, Table> getDataSourceTables() {
        return this.dataSourceTables;
    }

    /** Sets of data source mappings from tables to nodes and relationships, often used for importing. */
    @JsonProperty("data_source_mappings")
    public Map<String, List<Mapping>> getDataSourceMappings() {
        return this.dataSourceMappings;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GraphModelExtensions)) return false;
        GraphModelExtensions that = (GraphModelExtensions) other;
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
        return "GraphModelExtensions(dataSourceTables=" + dataSourceTables + ", dataSourceMappings="
                + dataSourceMappings + ", custom=" + custom + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private Map<String, Table> dataSourceTables = new LinkedHashMap<>();
        private Map<String, List<Mapping>> dataSourceMappings = new LinkedHashMap<>();
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        @JsonProperty("data_source_tables")
        public Builder dataSourceTables(Map<String, Table> dataSourceTables) {
            this.dataSourceTables = dataSourceTables;
            return this;
        }

        @JsonProperty("data_source_mappings")
        public Builder dataSourceMappings(Map<String, List<Mapping>> dataSourceMappings) {
            this.dataSourceMappings = dataSourceMappings;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public GraphModelExtensions build() {
            return new GraphModelExtensions(this);
        }
    }
}
