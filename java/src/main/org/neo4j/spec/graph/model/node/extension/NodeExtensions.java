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
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

@JsonClassDescription("Extensions for a node.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class NodeExtensions implements CustomExtensions {
    private final Map<String, NodeIndex> indexes;

    private final NodeDisplay display;

    private final Map<String, Object> custom;

    private NodeExtensions(Map<String, NodeIndex> indexes, NodeDisplay display, Map<String, Object> custom) {
        this.indexes = indexes;
        this.display = display;
        this.custom = custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, NodeIndex> getIndexes() {
        return this.indexes;
    }

    public NodeDisplay getDisplay() {
        return this.display;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NodeExtensions)) return false;
        NodeExtensions that = (NodeExtensions) other;
        return Objects.equals(indexes, that.indexes)
                && Objects.equals(display, that.display)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes, display, custom);
    }

    @Override
    public String toString() {
        return "NodeExtensions(indexes=" + indexes + ", display=" + display + ", custom=" + custom + ")";
    }

    @JsonProperty("indexes")
    @JsonPropertyDescription("Database indexes on this node keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, NodeIndex> jsonIndexes() {
        return Objects.equals(this.indexes, new LinkedHashMap<String, NodeIndex>()) ? null : this.indexes;
    }

    @JsonProperty("display")
    @JsonPropertyDescription("Optional visual information of this node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private NodeDisplay jsonDisplay() {
        return Objects.equals(this.display, null) ? null : this.display;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static NodeExtensions fromJson(
            @JsonProperty("indexes") Map<String, NodeIndex> indexes,
            @JsonProperty("display") NodeDisplay display,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new NodeExtensions(
                indexes != null ? indexes : new LinkedHashMap<>(),
                display,
                custom != null ? custom : new LinkedHashMap<>());
    }

    public static final class Builder {
        private Map<String, NodeIndex> indexes = new LinkedHashMap<>();
        private NodeDisplay display = null;
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        public Builder indexes(Map<String, NodeIndex> indexes) {
            this.indexes = indexes;
            return this;
        }

        public Builder display(NodeDisplay display) {
            this.display = display;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public NodeExtensions build() {
            return new NodeExtensions(indexes, display, custom);
        }
    }
}
