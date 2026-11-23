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

@JsonClassDescription("Extensions for a node constraint.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class NodeConstraintExtensions implements CustomExtensions {
    private final Map<String, Object> custom;

    private NodeConstraintExtensions(Map<String, Object> custom) {
        this.custom = custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NodeConstraintExtensions)) return false;
        NodeConstraintExtensions that = (NodeConstraintExtensions) other;
        return Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(custom);
    }

    @Override
    public String toString() {
        return "NodeConstraintExtensions(custom=" + custom + ")";
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static NodeConstraintExtensions fromJson(@JsonProperty("custom") Map<String, Object> custom) {
        return new NodeConstraintExtensions(custom != null ? custom : new LinkedHashMap<>());
    }

    public static final class Builder {
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public NodeConstraintExtensions build() {
            return new NodeConstraintExtensions(custom);
        }
    }
}
