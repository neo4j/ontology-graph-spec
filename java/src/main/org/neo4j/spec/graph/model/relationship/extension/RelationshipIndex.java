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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;
import org.neo4j.spec.graph.model.index.IndexOption;
import org.neo4j.spec.graph.model.type.IndexType;
import org.neo4j.spec.graph.model.type.Named;

/** An index on a relationship. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = RelationshipIndex.Builder.class)
public final class RelationshipIndex implements CustomExtensions, Named {
    private IndexType type;

    private final List<String> properties;

    private final IndexOption options;

    private final Map<String, Object> custom;

    private String name;

    /** Default instance Jackson compares against to omit default values. */
    private RelationshipIndex() {
        this(new Builder(null, null));
    }

    private RelationshipIndex(Builder builder) {
        this.type = builder.type;
        this.properties = builder.properties;
        this.options = builder.options;
        this.custom = builder.custom;
        this.name = builder.name;
    }

    public static Builder builder(IndexType type, List<String> properties) {
        return new Builder(type, properties);
    }

    /** Type of the index. */
    public IndexType getType() {
        return this.type;
    }

    public void setType(IndexType value) {
        this.type = value;
    }

    /** Ids of the properties the index covers. */
    public List<String> getProperties() {
        return this.properties;
    }

    /** Configuration specific to the type of index. */
    public IndexOption getOptions() {
        return this.options;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    /** Name of the index in the database. */
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
        if (!(other instanceof RelationshipIndex)) return false;
        RelationshipIndex that = (RelationshipIndex) other;
        return Objects.equals(type, that.type)
                && Objects.equals(properties, that.properties)
                && Objects.equals(options, that.options)
                && Objects.equals(custom, that.custom)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, properties, options, custom, name);
    }

    @Override
    public String toString() {
        return "RelationshipIndex(type=" + type + ", properties=" + properties + ", options=" + options + ", custom="
                + custom + ", name=" + name + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private IndexType type;
        private List<String> properties;
        private IndexOption options = null;
        private Map<String, Object> custom = new LinkedHashMap<>();
        private String name = null;

        @JsonCreator
        private Builder(@JsonProperty("type") IndexType type, @JsonProperty("properties") List<String> properties) {
            this.type = type;
            this.properties = properties;
        }

        public Builder type(IndexType type) {
            this.type = type;
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

        public RelationshipIndex build() {
            Objects.requireNonNull(type, "Field 'type' is required");
            Objects.requireNonNull(properties, "Field 'properties' is required");
            return new RelationshipIndex(this);
        }
    }
}
