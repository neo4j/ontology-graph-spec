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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Maps rows of a table to relationships. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({
    "relationship",
    "table",
    "from_node",
    "to_node",
    "match_label",
    "properties",
    "mode",
    "key",
    "depends_on",
    "id"
})
@JsonDeserialize(builder = RelationshipMapping.Builder.class)
@JsonTypeName("RELATIONSHIP")
public final class RelationshipMapping implements Mapping {
    private String relationship;

    private String table;

    private final TargetMapping fromNode;

    private final TargetMapping toNode;

    private String matchLabel;

    private final Map<String, PropertyMapping> properties;

    private MappingMode mode;

    private final List<String> key;

    private String dependsOn;

    private String id;

    /** Default instance Jackson compares against to omit default values. */
    private RelationshipMapping() {
        this(new Builder(null, null, null, null));
    }

    private RelationshipMapping(Builder builder) {
        this.relationship = builder.relationship;
        this.table = builder.table;
        this.fromNode = builder.fromNode;
        this.toNode = builder.toNode;
        this.matchLabel = builder.matchLabel;
        this.properties = builder.properties;
        this.mode = builder.mode;
        this.key = builder.key;
        this.dependsOn = builder.dependsOn;
        this.id = builder.id;
    }

    public static Builder builder(String relationship, String table, TargetMapping fromNode, TargetMapping toNode) {
        return new Builder(relationship, table, fromNode, toNode);
    }

    /** Id of the relationship being mapped. */
    public String getRelationship() {
        return this.relationship;
    }

    public void setRelationship(String value) {
        this.relationship = value;
    }

    /** Id of the source table. */
    public String getTable() {
        return this.table;
    }

    public void setTable(String value) {
        this.table = value;
    }

    /** How the start node of the relationship is matched. */
    @JsonProperty("from_node")
    public TargetMapping getFromNode() {
        return this.fromNode;
    }

    /** How the end node of the relationship is matched. */
    @JsonProperty("to_node")
    public TargetMapping getToNode() {
        return this.toNode;
    }

    /** Label used to match existing relationships. */
    @JsonProperty("match_label")
    public String getMatchLabel() {
        return this.matchLabel;
    }

    public void setMatchLabel(String value) {
        this.matchLabel = value;
    }

    /** Mappings from property ids to source columns. */
    public Map<String, PropertyMapping> getProperties() {
        return this.properties;
    }

    /** Whether relationships are merged with existing relationships or always created. */
    public MappingMode getMode() {
        return this.mode;
    }

    public void setMode(MappingMode value) {
        this.mode = value;
    }

    /** Ids of the properties which identify a relationship. */
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
        if (!(other instanceof RelationshipMapping)) return false;
        RelationshipMapping that = (RelationshipMapping) other;
        return Objects.equals(relationship, that.relationship)
                && Objects.equals(table, that.table)
                && Objects.equals(fromNode, that.fromNode)
                && Objects.equals(toNode, that.toNode)
                && Objects.equals(matchLabel, that.matchLabel)
                && Objects.equals(properties, that.properties)
                && Objects.equals(mode, that.mode)
                && Objects.equals(key, that.key)
                && Objects.equals(dependsOn, that.dependsOn)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationship, table, fromNode, toNode, matchLabel, properties, mode, key, dependsOn, id);
    }

    @Override
    public String toString() {
        return "RelationshipMapping(relationship=" + relationship + ", table=" + table + ", fromNode=" + fromNode
                + ", toNode=" + toNode + ", matchLabel=" + matchLabel + ", properties=" + properties + ", mode=" + mode
                + ", key=" + key + ", dependsOn=" + dependsOn + ", id=" + id + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String relationship;
        private String table;
        private TargetMapping fromNode;
        private TargetMapping toNode;
        private String matchLabel = null;
        private Map<String, PropertyMapping> properties = new LinkedHashMap<>();
        private MappingMode mode = MappingMode.MERGE;
        private List<String> key = new ArrayList<>();
        private String dependsOn = "";
        private String id = "";

        @JsonCreator
        private Builder(
                @JsonProperty("relationship") String relationship,
                @JsonProperty("table") String table,
                @JsonProperty("from_node") TargetMapping fromNode,
                @JsonProperty("to_node") TargetMapping toNode) {
            this.relationship = relationship;
            this.table = table;
            this.fromNode = fromNode;
            this.toNode = toNode;
        }

        public Builder relationship(String relationship) {
            this.relationship = relationship;
            return this;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        @JsonProperty("from_node")
        public Builder fromNode(TargetMapping fromNode) {
            this.fromNode = fromNode;
            return this;
        }

        @JsonProperty("to_node")
        public Builder toNode(TargetMapping toNode) {
            this.toNode = toNode;
            return this;
        }

        @JsonProperty("match_label")
        public Builder matchLabel(String matchLabel) {
            this.matchLabel = matchLabel;
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

        public RelationshipMapping build() {
            Objects.requireNonNull(relationship, "Field 'relationship' is required");
            Objects.requireNonNull(table, "Field 'table' is required");
            Objects.requireNonNull(fromNode, "Field 'from_node' is required");
            Objects.requireNonNull(toNode, "Field 'to_node' is required");
            return new RelationshipMapping(this);
        }
    }
}
