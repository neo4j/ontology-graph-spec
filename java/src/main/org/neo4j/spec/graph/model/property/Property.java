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
package org.neo4j.spec.graph.model.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.property.extension.PropertyExtensions;
import org.neo4j.spec.graph.model.type.Named;

/** A property of a node or relationship. */
public final class Property implements Named {
    private Neo4jType type;

    private Integer dimension;

    private Boolean mustExist;

    private Boolean unique;

    private Boolean key;

    private final PropertyExtensions extensions;

    private String name;

    private final String description;

    private final List<String> aliases;

    private final String reference;

    private final String pattern;

    private final List<String> oneOf;

    private Property(Builder builder) {
        this.type = builder.type;
        this.dimension = builder.dimension;
        this.mustExist = builder.mustExist;
        this.unique = builder.unique;
        this.key = builder.key;
        this.extensions = builder.extensions;
        this.name = builder.name;
        this.description = builder.description;
        this.aliases = builder.aliases;
        this.reference = builder.reference;
        this.pattern = builder.pattern;
        this.oneOf = builder.oneOf;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Neo4j type of the property. */
    public Neo4jType getType() {
        return this.type;
    }

    public void setType(Neo4jType value) {
        this.type = value;
    }

    /** Dimension of the property when it holds a vector. */
    public Integer getDimension() {
        return this.dimension;
    }

    public void setDimension(Integer value) {
        this.dimension = value;
    }

    /** Shorthand for an existence constraint. Expanded into constraints when the model is decoded. */
    public Boolean getMustExist() {
        return this.mustExist;
    }

    public void setMustExist(Boolean value) {
        this.mustExist = value;
    }

    /** Shorthand for a uniqueness constraint. Expanded into constraints when the model is decoded. */
    public Boolean getUnique() {
        return this.unique;
    }

    public void setUnique(Boolean value) {
        this.unique = value;
    }

    /** Shorthand for a key constraint. Expanded into constraints when the model is decoded. */
    public Boolean getKey() {
        return this.key;
    }

    public void setKey(Boolean value) {
        this.key = value;
    }

    /** Extensions for the property. */
    public PropertyExtensions getExtensions() {
        return this.extensions;
    }

    /** Name of the property in the database, when different from its id. */
    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    /** Description of the property. */
    public String getDescription() {
        return this.description;
    }

    /** Alternative names for the property. */
    public List<String> getAliases() {
        return this.aliases;
    }

    /** Reference or URI of an external resource describing the property. */
    public String getReference() {
        return this.reference;
    }

    /** Pattern values of the property are expected to match. */
    public String getPattern() {
        return this.pattern;
    }

    /** Values the property is restricted to. */
    public List<String> getOneOf() {
        return this.oneOf;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Property)) return false;
        Property that = (Property) other;
        return Objects.equals(type, that.type)
                && Objects.equals(dimension, that.dimension)
                && Objects.equals(mustExist, that.mustExist)
                && Objects.equals(unique, that.unique)
                && Objects.equals(key, that.key)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(aliases, that.aliases)
                && Objects.equals(reference, that.reference)
                && Objects.equals(pattern, that.pattern)
                && Objects.equals(oneOf, that.oneOf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type,
                dimension,
                mustExist,
                unique,
                key,
                extensions,
                name,
                description,
                aliases,
                reference,
                pattern,
                oneOf);
    }

    @Override
    public String toString() {
        return "Property(type=" + type + ", dimension=" + dimension + ", mustExist=" + mustExist + ", unique=" + unique
                + ", key=" + key + ", extensions=" + extensions + ", name=" + name + ", description=" + description
                + ", aliases=" + aliases + ", reference=" + reference + ", pattern=" + pattern + ", oneOf=" + oneOf
                + ")";
    }

    public static final class Builder {
        private Neo4jType type = Neo4jType.ANY;
        private Integer dimension = null;
        private Boolean mustExist = null;
        private Boolean unique = null;
        private Boolean key = null;
        private PropertyExtensions extensions = PropertyExtensions.builder().build();
        private String name = null;
        private String description = "";
        private List<String> aliases = new ArrayList<>();
        private String reference = "";
        private String pattern = "";
        private List<String> oneOf = new ArrayList<>();

        private Builder() {}

        public Builder type(Neo4jType type) {
            this.type = type;
            return this;
        }

        public Builder dimension(Integer dimension) {
            this.dimension = dimension;
            return this;
        }

        public Builder mustExist(Boolean mustExist) {
            this.mustExist = mustExist;
            return this;
        }

        public Builder unique(Boolean unique) {
            this.unique = unique;
            return this;
        }

        public Builder key(Boolean key) {
            this.key = key;
            return this;
        }

        public Builder extensions(PropertyExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder oneOf(List<String> oneOf) {
            this.oneOf = oneOf;
            return this;
        }

        public Property build() {
            return new Property(this);
        }
    }
}
