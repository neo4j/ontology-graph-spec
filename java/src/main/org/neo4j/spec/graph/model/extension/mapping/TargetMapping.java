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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@JsonClassDescription("Identifies a node at one end of a mapped relationship.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class TargetMapping {
    private String label;

    private final Map<String, PropertyMapping> properties;

    public TargetMapping(String label, Map<String, PropertyMapping> properties) {
        this.label = label;
        this.properties = properties;
    }

    public TargetMapping(String label) {
        this(label, new LinkedHashMap<String, PropertyMapping>());
    }

    public TargetMapping() {
        this("", new LinkedHashMap<String, PropertyMapping>());
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    public Map<String, PropertyMapping> getProperties() {
        return this.properties;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TargetMapping)) return false;
        TargetMapping that = (TargetMapping) other;
        return Objects.equals(label, that.label) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(properties);
        return result;
    }

    @Override
    public String toString() {
        return "TargetMapping(label=" + label + ", properties=" + properties + ")";
    }

    @JsonProperty("label")
    @JsonPropertyDescription("Label of the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonLabel() {
        return Objects.equals(this.label, "") ? null : this.label;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Mappings from property ids to source columns used to match the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PropertyMapping> jsonProperties() {
        return Objects.equals(this.properties, new LinkedHashMap<String, PropertyMapping>()) ? null : this.properties;
    }

    @JsonCreator
    private static TargetMapping fromJson(
            @JsonProperty("label") String label, @JsonProperty("properties") Map<String, PropertyMapping> properties) {
        return new TargetMapping(
                label != null ? label : "",
                properties != null ? properties : new LinkedHashMap<String, PropertyMapping>());
    }
}
