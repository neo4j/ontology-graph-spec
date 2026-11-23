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
package org.neo4j.spec.graph.model.node.extension;

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
import org.neo4j.spec.graph.model.extension.CustomExtensions;
import org.neo4j.spec.graph.model.index.IndexOption;
import org.neo4j.spec.graph.model.type.IndexType;
import org.neo4j.spec.graph.model.type.Named;

@JsonClassDescription("An index on a node.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class NodeIndex implements CustomExtensions, Named {
    private IndexType type;

    private final List<String> labels;

    private final List<String> properties;

    private final IndexOption options;

    private final Map<String, Object> custom;

    private String name;

    private NodeIndex(
            IndexType type,
            List<String> labels,
            List<String> properties,
            IndexOption options,
            Map<String, Object> custom,
            String name) {
        this.type = type;
        this.labels = labels;
        this.properties = properties;
        this.options = options;
        this.custom = custom;
        this.name = name;
    }

    public static Builder builder(IndexType type, List<String> labels, List<String> properties) {
        return new Builder(type, labels, properties);
    }

    public IndexType getType() {
        return this.type;
    }

    public void setType(IndexType value) {
        this.type = value;
    }

    public List<String> getLabels() {
        return this.labels;
    }

    public List<String> getProperties() {
        return this.properties;
    }

    public IndexOption getOptions() {
        return this.options;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NodeIndex)) return false;
        NodeIndex that = (NodeIndex) other;
        return Objects.equals(type, that.type)
                && Objects.equals(labels, that.labels)
                && Objects.equals(properties, that.properties)
                && Objects.equals(options, that.options)
                && Objects.equals(custom, that.custom)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(labels);
        result = 31 * result + Objects.hashCode(properties);
        result = 31 * result + Objects.hashCode(options);
        result = 31 * result + Objects.hashCode(custom);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }

    @Override
    public String toString() {
        return "NodeIndex(type=" + type + ", labels=" + labels + ", properties=" + properties + ", options=" + options
                + ", custom=" + custom + ", name=" + name + ")";
    }

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the index.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private IndexType jsonType() {
        return this.type;
    }

    @JsonProperty("labels")
    @JsonPropertyDescription("Labels the index covers.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<String> jsonLabels() {
        return this.labels;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Ids of the properties the index covers.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<String> jsonProperties() {
        return this.properties;
    }

    @JsonProperty("options")
    @JsonPropertyDescription("Configuration specific to the type of index.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private IndexOption jsonOptions() {
        return Objects.equals(this.options, null) ? null : this.options;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonProperty("name")
    @JsonPropertyDescription("Name of the index in the database.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonName() {
        return Objects.equals(this.name, null) ? null : this.name;
    }

    @JsonCreator
    private static NodeIndex fromJson(
            @JsonProperty("type") IndexType type,
            @JsonProperty("labels") List<String> labels,
            @JsonProperty("properties") List<String> properties,
            @JsonProperty("options") IndexOption options,
            @JsonProperty("custom") Map<String, Object> custom,
            @JsonProperty("name") String name) {
        return new NodeIndex(
                Objects.requireNonNull(type, "Field 'type' is required"),
                Objects.requireNonNull(labels, "Field 'labels' is required"),
                Objects.requireNonNull(properties, "Field 'properties' is required"),
                options,
                custom != null ? custom : new LinkedHashMap<>(),
                name);
    }

    public static final class Builder {
        private IndexType type;
        private List<String> labels;
        private List<String> properties;
        private IndexOption options = null;
        private Map<String, Object> custom = new LinkedHashMap<>();
        private String name = null;

        private Builder(IndexType type, List<String> labels, List<String> properties) {
            this.type = type;
            this.labels = labels;
            this.properties = properties;
        }

        public Builder type(IndexType type) {
            this.type = type;
            return this;
        }

        public Builder labels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder properties(List<String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder options(IndexOption options) {
            this.options = options;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public NodeIndex build() {
            return new NodeIndex(type, labels, properties, options, custom, name);
        }
    }
}
