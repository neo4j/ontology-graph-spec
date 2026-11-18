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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.mapping.Mapping;
import org.neo4j.spec.graph.model.extension.source.Table;

/**
 * Extensions for a {@code GraphModel}.
 */
@JsonClassDescription("Extensions for a graph model.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class GraphModelExtensions implements CustomExtensions {
    private final Map<String, Table> dataSourceTables;

    private final Map<String, List<Mapping>> dataSourceMappings;

    private final Map<String, Object> custom;

    public GraphModelExtensions(
            Map<String, Table> dataSourceTables,
            Map<String, List<Mapping>> dataSourceMappings,
            Map<String, Object> custom) {
        this.dataSourceTables = dataSourceTables;
        this.dataSourceMappings = dataSourceMappings;
        this.custom = custom;
    }

    public GraphModelExtensions(Map<String, Table> dataSourceTables, Map<String, List<Mapping>> dataSourceMappings) {
        this(dataSourceTables, dataSourceMappings, new LinkedHashMap<String, Object>());
    }

    public GraphModelExtensions(Map<String, Table> dataSourceTables) {
        this(dataSourceTables, new LinkedHashMap<String, List<Mapping>>(), new LinkedHashMap<String, Object>());
    }

    public GraphModelExtensions() {
        this(
                new LinkedHashMap<String, Table>(),
                new LinkedHashMap<String, List<Mapping>>(),
                new LinkedHashMap<String, Object>());
    }

    public Map<String, Table> getDataSourceTables() {
        return this.dataSourceTables;
    }

    public Map<String, List<Mapping>> getDataSourceMappings() {
        return this.dataSourceMappings;
    }

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
        int result = Objects.hashCode(dataSourceTables);
        result = 31 * result + Objects.hashCode(dataSourceMappings);
        result = 31 * result + Objects.hashCode(custom);
        return result;
    }

    @Override
    public String toString() {
        return "GraphModelExtensions(dataSourceTables=" + dataSourceTables + ", dataSourceMappings="
                + dataSourceMappings + ", custom=" + custom + ")";
    }

    @JsonProperty("data_source_tables")
    @JsonPropertyDescription("Data source table schemas keyed by table id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Table> jsonDataSourceTables() {
        return Objects.equals(this.dataSourceTables, new LinkedHashMap<String, Table>()) ? null : this.dataSourceTables;
    }

    @JsonProperty("data_source_mappings")
    @JsonPropertyDescription(
            "Sets of data source mappings from tables to nodes and relationships, often used for importing.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, List<Mapping>> jsonDataSourceMappings() {
        return Objects.equals(this.dataSourceMappings, new LinkedHashMap<String, List<Mapping>>())
                ? null
                : this.dataSourceMappings;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static GraphModelExtensions fromJson(
            @JsonProperty("data_source_tables") Map<String, Table> dataSourceTables,
            @JsonProperty("data_source_mappings") Map<String, List<Mapping>> dataSourceMappings,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new GraphModelExtensions(
                dataSourceTables != null ? dataSourceTables : new LinkedHashMap<String, Table>(),
                dataSourceMappings != null ? dataSourceMappings : new LinkedHashMap<String, List<Mapping>>(),
                custom != null ? custom : new LinkedHashMap<String, Object>());
    }
}
