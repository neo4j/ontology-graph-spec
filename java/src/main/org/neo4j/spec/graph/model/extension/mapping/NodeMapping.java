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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Maps rows of a table to nodes. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({"node", "table", "properties", "mode", "match_label", "key", "depends_on", "id"})
@JsonDeserialize(builder = NodeMapping.Builder.class)
@JsonTypeName("NODE")
public final class NodeMapping implements Mapping {
    private String node;

    private String table;

    private final Map<String, PropertyMapping> properties;

    private MappingMode mode;

    private String matchLabel;

    private final List<String> key;

    private String dependsOn;

    private String id;

    /** Default instance Jackson compares against to omit default values. */
    private NodeMapping() {
        this(new Builder(null, null, null));
    }

    private NodeMapping(Builder builder) {
        this.node = builder.node;
        this.table = builder.table;
        this.properties = builder.properties;
        this.mode = builder.mode;
        this.matchLabel = builder.matchLabel;
        this.key = builder.key;
        this.dependsOn = builder.dependsOn;
        this.id = builder.id;
    }

    public static Builder builder(String node, String table, Map<String, PropertyMapping> properties) {
        return new Builder(node, table, properties);
    }

    /** Id of the node being mapped. */
    public String getNode() {
        return this.node;
    }

    public void setNode(String value) {
        this.node = value;
    }

    /** Id of the source table. */
    public String getTable() {
        return this.table;
    }

    public void setTable(String value) {
        this.table = value;
    }

    /** Mappings from property ids to source columns. */
    public Map<String, PropertyMapping> getProperties() {
        return this.properties;
    }

    /** Whether nodes are merged with existing nodes or always created. */
    public MappingMode getMode() {
        return this.mode;
    }

    public void setMode(MappingMode value) {
        this.mode = value;
    }

    /** Label used to match existing nodes. */
    @JsonProperty("match_label")
    public String getMatchLabel() {
        return this.matchLabel;
    }

    public void setMatchLabel(String value) {
        this.matchLabel = value;
    }

    /** Ids of the properties which identify a node. */
    public List<String> getKey() {
        return this.key;
    }

    /** Id of another mapping in the same list which must be imported before this one. Empty when there is no dependency. */
    @JsonProperty("depends_on")
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
        return Objects.hash(node, table, properties, mode, matchLabel, key, dependsOn, id);
    }

    @Override
    public String toString() {
        return "NodeMapping(node=" + node + ", table=" + table + ", properties=" + properties + ", mode=" + mode
                + ", matchLabel=" + matchLabel + ", key=" + key + ", dependsOn=" + dependsOn + ", id=" + id + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String node;
        private String table;
        private Map<String, PropertyMapping> properties;
        private MappingMode mode = MappingMode.MERGE;
        private String matchLabel = null;
        private List<String> key = new ArrayList<>();
        private String dependsOn = "";
        private String id = "";

        @JsonCreator
        private Builder(
                @JsonProperty("node") String node,
                @JsonProperty("table") String table,
                @JsonProperty("properties") Map<String, PropertyMapping> properties) {
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

        @JsonProperty("match_label")
        public Builder matchLabel(String matchLabel) {
            this.matchLabel = matchLabel;
            return this;
        }

        public Builder key(List<String> key) {
            this.key = key;
            return this;
        }

        @JsonProperty("depends_on")
        public Builder dependsOn(String dependsOn) {
            this.dependsOn = dependsOn;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public NodeMapping build() {
            Objects.requireNonNull(node, "Field 'node' is required");
            Objects.requireNonNull(table, "Field 'table' is required");
            Objects.requireNonNull(properties, "Field 'properties' is required");
            return new NodeMapping(this);
        }
    }
}
