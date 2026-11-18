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
// Generated from org/neo4j/spec/graph/model/extension/mapping/RelationshipMapping.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.extension.mapping;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonClassDescription("Maps rows of a table to relationships.")
@JsonTypeName("RELATIONSHIP")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
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

    public RelationshipMapping(
            String relationship,
            String table,
            TargetMapping fromNode,
            TargetMapping toNode,
            String matchLabel,
            Map<String, PropertyMapping> properties,
            MappingMode mode,
            List<String> key,
            String dependsOn,
            String id) {
        this.relationship = relationship;
        this.table = table;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.matchLabel = matchLabel;
        this.properties = properties;
        this.mode = mode;
        this.key = key;
        this.dependsOn = dependsOn;
        this.id = id;
    }

    public RelationshipMapping(
            String relationship,
            String table,
            TargetMapping fromNode,
            TargetMapping toNode,
            String matchLabel,
            Map<String, PropertyMapping> properties,
            MappingMode mode,
            List<String> key,
            String dependsOn) {
        this(relationship, table, fromNode, toNode, matchLabel, properties, mode, key, dependsOn, "");
    }

    public RelationshipMapping(
            String relationship,
            String table,
            TargetMapping fromNode,
            TargetMapping toNode,
            String matchLabel,
            Map<String, PropertyMapping> properties,
            MappingMode mode,
            List<String> key) {
        this(relationship, table, fromNode, toNode, matchLabel, properties, mode, key, "", "");
    }

    public RelationshipMapping(
            String relationship,
            String table,
            TargetMapping fromNode,
            TargetMapping toNode,
            String matchLabel,
            Map<String, PropertyMapping> properties,
            MappingMode mode) {
        this(relationship, table, fromNode, toNode, matchLabel, properties, mode, new ArrayList<String>(), "", "");
    }

    public RelationshipMapping(
            String relationship,
            String table,
            TargetMapping fromNode,
            TargetMapping toNode,
            String matchLabel,
            Map<String, PropertyMapping> properties) {
        this(
                relationship,
                table,
                fromNode,
                toNode,
                matchLabel,
                properties,
                MappingMode.MERGE,
                new ArrayList<String>(),
                "",
                "");
    }

    public RelationshipMapping(
            String relationship, String table, TargetMapping fromNode, TargetMapping toNode, String matchLabel) {
        this(
                relationship,
                table,
                fromNode,
                toNode,
                matchLabel,
                new LinkedHashMap<String, PropertyMapping>(),
                MappingMode.MERGE,
                new ArrayList<String>(),
                "",
                "");
    }

    public RelationshipMapping(String relationship, String table, TargetMapping fromNode, TargetMapping toNode) {
        this(
                relationship,
                table,
                fromNode,
                toNode,
                null,
                new LinkedHashMap<String, PropertyMapping>(),
                MappingMode.MERGE,
                new ArrayList<String>(),
                "",
                "");
    }

    public String getRelationship() {
        return this.relationship;
    }

    public void setRelationship(String value) {
        this.relationship = value;
    }

    public String getTable() {
        return this.table;
    }

    public void setTable(String value) {
        this.table = value;
    }

    public TargetMapping getFromNode() {
        return this.fromNode;
    }

    public TargetMapping getToNode() {
        return this.toNode;
    }

    public String getMatchLabel() {
        return this.matchLabel;
    }

    public void setMatchLabel(String value) {
        this.matchLabel = value;
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
        int result = Objects.hashCode(relationship);
        result = 31 * result + Objects.hashCode(table);
        result = 31 * result + Objects.hashCode(fromNode);
        result = 31 * result + Objects.hashCode(toNode);
        result = 31 * result + Objects.hashCode(matchLabel);
        result = 31 * result + Objects.hashCode(properties);
        result = 31 * result + Objects.hashCode(mode);
        result = 31 * result + Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(dependsOn);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }

    @Override
    public String toString() {
        return "RelationshipMapping(relationship=" + relationship + ", table=" + table + ", fromNode=" + fromNode
                + ", toNode=" + toNode + ", matchLabel=" + matchLabel + ", properties=" + properties + ", mode=" + mode
                + ", key=" + key + ", dependsOn=" + dependsOn + ", id=" + id + ")";
    }

    @JsonProperty("relationship")
    @JsonPropertyDescription("Id of the relationship being mapped.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonRelationship() {
        return this.relationship;
    }

    @JsonProperty("table")
    @JsonPropertyDescription("Id of the source table.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonTable() {
        return this.table;
    }

    @JsonProperty("from_node")
    @JsonPropertyDescription("How the start node of the relationship is matched.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private TargetMapping jsonFromNode() {
        return this.fromNode;
    }

    @JsonProperty("to_node")
    @JsonPropertyDescription("How the end node of the relationship is matched.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private TargetMapping jsonToNode() {
        return this.toNode;
    }

    @JsonProperty("match_label")
    @JsonPropertyDescription("Label used to match existing relationships.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonMatchLabel() {
        return Objects.equals(this.matchLabel, null) ? null : this.matchLabel;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Mappings from property ids to source columns.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PropertyMapping> jsonProperties() {
        return Objects.equals(this.properties, new LinkedHashMap<String, PropertyMapping>()) ? null : this.properties;
    }

    @JsonProperty("mode")
    @JsonPropertyDescription("Whether relationships are merged with existing relationships or always created.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MappingMode jsonMode() {
        return Objects.equals(this.mode, MappingMode.MERGE) ? null : this.mode;
    }

    @JsonProperty("key")
    @JsonPropertyDescription("Ids of the properties which identify a relationship.")
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
    private static RelationshipMapping fromJson(
            @JsonProperty("relationship") String relationship,
            @JsonProperty("table") String table,
            @JsonProperty("from_node") TargetMapping fromNode,
            @JsonProperty("to_node") TargetMapping toNode,
            @JsonProperty("match_label") String matchLabel,
            @JsonProperty("properties") Map<String, PropertyMapping> properties,
            @JsonProperty("mode") MappingMode mode,
            @JsonProperty("key") List<String> key,
            @JsonProperty("depends_on") String dependsOn,
            @JsonProperty("id") String id) {
        return new RelationshipMapping(
                Objects.requireNonNull(relationship, "Field 'relationship' is required"),
                Objects.requireNonNull(table, "Field 'table' is required"),
                Objects.requireNonNull(fromNode, "Field 'from_node' is required"),
                Objects.requireNonNull(toNode, "Field 'to_node' is required"),
                matchLabel != null ? matchLabel : null,
                properties != null ? properties : new LinkedHashMap<String, PropertyMapping>(),
                mode != null ? mode : MappingMode.MERGE,
                key != null ? key : new ArrayList<String>(),
                dependsOn != null ? dependsOn : "",
                id != null ? id : "");
    }
}
