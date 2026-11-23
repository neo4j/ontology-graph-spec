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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonClassDescription("Maps rows of a table to nodes.")
@JsonTypeName("NODE")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class NodeMapping implements Mapping {
    private String node;

    private String table;

    private final Map<String, PropertyMapping> properties;

    private MappingMode mode;

    private String matchLabel;

    private final List<String> key;

    private String dependsOn;

    private String id;

    private NodeMapping(
            String node,
            String table,
            Map<String, PropertyMapping> properties,
            MappingMode mode,
            String matchLabel,
            List<String> key,
            String dependsOn,
            String id) {
        this.node = node;
        this.table = table;
        this.properties = properties;
        this.mode = mode;
        this.matchLabel = matchLabel;
        this.key = key;
        this.dependsOn = dependsOn;
        this.id = id;
    }

    public static Builder builder(String node, String table, Map<String, PropertyMapping> properties) {
        return new Builder(node, table, properties);
    }

    public String getNode() {
        return this.node;
    }

    public void setNode(String value) {
        this.node = value;
    }

    public String getTable() {
        return this.table;
    }

    public void setTable(String value) {
        this.table = value;
    }

    public Map<String, PropertyMapping> getProperties() {
        return this.properties;
    }

    public MappingMode getMode() {
        return this.mode;
    }

    public void setMode(MappingMode value) {
        this.mode = value;
    }

    public String getMatchLabel() {
        return this.matchLabel;
    }

    public void setMatchLabel(String value) {
        this.matchLabel = value;
    }

    public List<String> getKey() {
        return this.key;
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
        if (!(other instanceof NodeMapping)) return false;
        NodeMapping that = (NodeMapping) other;
        return Objects.equals(node, that.node)
                && Objects.equals(table, that.table)
                && Objects.equals(properties, that.properties)
                && Objects.equals(mode, that.mode)
                && Objects.equals(matchLabel, that.matchLabel)
                && Objects.equals(key, that.key)
                && Objects.equals(dependsOn, that.dependsOn)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(node);
        result = 31 * result + Objects.hashCode(table);
        result = 31 * result + Objects.hashCode(properties);
        result = 31 * result + Objects.hashCode(mode);
        result = 31 * result + Objects.hashCode(matchLabel);
        result = 31 * result + Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(dependsOn);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }

    @Override
    public String toString() {
        return "NodeMapping(node=" + node + ", table=" + table + ", properties=" + properties + ", mode=" + mode
                + ", matchLabel=" + matchLabel + ", key=" + key + ", dependsOn=" + dependsOn + ", id=" + id + ")";
    }

    @JsonProperty("node")
    @JsonPropertyDescription("Id of the node being mapped.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonNode() {
        return this.node;
    }

    @JsonProperty("table")
    @JsonPropertyDescription("Id of the source table.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonTable() {
        return this.table;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Mappings from property ids to source columns.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Map<String, PropertyMapping> jsonProperties() {
        return this.properties;
    }

    @JsonProperty("mode")
    @JsonPropertyDescription("Whether nodes are merged with existing nodes or always created.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MappingMode jsonMode() {
        return Objects.equals(this.mode, MappingMode.MERGE) ? null : this.mode;
    }

    @JsonProperty("match_label")
    @JsonPropertyDescription("Label used to match existing nodes.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonMatchLabel() {
        return Objects.equals(this.matchLabel, null) ? null : this.matchLabel;
    }

    @JsonProperty("key")
    @JsonPropertyDescription("Ids of the properties which identify a node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonKey() {
        return Objects.equals(this.key, new ArrayList<String>()) ? null : this.key;
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
    private static NodeMapping fromJson(
            @JsonProperty("node") String node,
            @JsonProperty("table") String table,
            @JsonProperty("properties") Map<String, PropertyMapping> properties,
            @JsonProperty("mode") MappingMode mode,
            @JsonProperty("match_label") String matchLabel,
            @JsonProperty("key") List<String> key,
            @JsonProperty("depends_on") String dependsOn,
            @JsonProperty("id") String id) {
        return new NodeMapping(
                Objects.requireNonNull(node, "Field 'node' is required"),
                Objects.requireNonNull(table, "Field 'table' is required"),
                Objects.requireNonNull(properties, "Field 'properties' is required"),
                mode != null ? mode : MappingMode.MERGE,
                matchLabel != null ? matchLabel : null,
                key != null ? key : new ArrayList<String>(),
                dependsOn != null ? dependsOn : "",
                id != null ? id : "");
    }

    public static final class Builder {
        private String node;
        private String table;
        private Map<String, PropertyMapping> properties;
        private MappingMode mode = MappingMode.MERGE;
        private String matchLabel = null;
        private List<String> key = new ArrayList<String>();
        private String dependsOn = "";
        private String id = "";

        private Builder(String node, String table, Map<String, PropertyMapping> properties) {
            this.node = node;
            this.table = table;
            this.properties = properties;
        }

        public Builder node(String node) {
            this.node = node;
            return this;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder properties(Map<String, PropertyMapping> properties) {
            this.properties = properties;
            return this;
        }

        public Builder mode(MappingMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder matchLabel(String matchLabel) {
            this.matchLabel = matchLabel;
            return this;
        }

        public Builder key(List<String> key) {
            this.key = key;
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

        public NodeMapping build() {
            return new NodeMapping(node, table, properties, mode, matchLabel, key, dependsOn, id);
        }
    }
}
