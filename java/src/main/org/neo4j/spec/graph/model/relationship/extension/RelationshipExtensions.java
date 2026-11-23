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
package org.neo4j.spec.graph.model.relationship.extension;

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

@JsonClassDescription("Extensions for a relationship.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class RelationshipExtensions implements CustomExtensions {
    private final Map<String, RelationshipIndex> indexes;

    private final Map<String, Object> custom;

    private RelationshipExtensions(Map<String, RelationshipIndex> indexes, Map<String, Object> custom) {
        this.indexes = indexes;
        this.custom = custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, RelationshipIndex> getIndexes() {
        return this.indexes;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RelationshipExtensions)) return false;
        RelationshipExtensions that = (RelationshipExtensions) other;
        return Objects.equals(indexes, that.indexes) && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(indexes);
        result = 31 * result + Objects.hashCode(custom);
        return result;
    }

    @Override
    public String toString() {
        return "RelationshipExtensions(indexes=" + indexes + ", custom=" + custom + ")";
    }

    @JsonProperty("indexes")
    @JsonPropertyDescription("Indexes on this relationship keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, RelationshipIndex> jsonIndexes() {
        return Objects.equals(this.indexes, new LinkedHashMap<String, RelationshipIndex>()) ? null : this.indexes;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static RelationshipExtensions fromJson(
            @JsonProperty("indexes") Map<String, RelationshipIndex> indexes,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new RelationshipExtensions(
                indexes != null ? indexes : new LinkedHashMap<>(), custom != null ? custom : new LinkedHashMap<>());
    }

    public static final class Builder {
        private Map<String, RelationshipIndex> indexes = new LinkedHashMap<>();
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        public Builder indexes(Map<String, RelationshipIndex> indexes) {
            this.indexes = indexes;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public RelationshipExtensions build() {
            return new RelationshipExtensions(indexes, custom);
        }
    }
}
