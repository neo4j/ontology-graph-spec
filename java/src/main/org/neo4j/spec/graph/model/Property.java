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
package org.neo4j.spec.graph.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A property of a node or relationship. */
public final class Property {
    private final Neo4jType type;
    private final Integer dimension;
    private final PropertyExtensions extensions;
    private final String name;
    private final List<String> aliases;
    private final String description;
    private final String reference;
    private final String pattern;
    private final List<String> oneOf;

    private Property(Builder b) {
        this.type = b.type;
        this.dimension = b.dimension;
        this.extensions = b.extensions;
        this.name = b.name;
        this.aliases = Collections.unmodifiableList(new ArrayList<>(b.aliases));
        this.description = b.description;
        this.reference = b.reference;
        this.pattern = b.pattern;
        this.oneOf = Collections.unmodifiableList(new ArrayList<>(b.oneOf));
    }

    /**
     * Creates an empty builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialised with the values of this instance.
     *
     * @return a new builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** @return neo4j type of the property. */
    public Optional<Neo4jType> type() {
        return Optional.ofNullable(type);
    }

    /** @return dimension of a vector typed property. */
    public Optional<Integer> dimension() {
        return Optional.ofNullable(dimension);
    }

    /** @return extensions for the property. */
    public Optional<PropertyExtensions> extensions() {
        return Optional.ofNullable(extensions);
    }

    /** @return name of the property in the graph, when different from its id. */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /** @return alternative names for the property. */
    public List<String> aliases() {
        return aliases;
    }

    /** @return description of the property. */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /** @return reference or URI of an external resource describing the property. */
    public Optional<String> reference() {
        return Optional.ofNullable(reference);
    }

    /** @return regular expression values must match. */
    public Optional<String> pattern() {
        return Optional.ofNullable(pattern);
    }

    /** @return values the property is restricted to (wire key {@code one_of}). */
    public List<String> oneOf() {
        return oneOf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Property that = (Property) o;
        return Objects.equals(type, that.type)
                && Objects.equals(dimension, that.dimension)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(name, that.name)
                && Objects.equals(aliases, that.aliases)
                && Objects.equals(description, that.description)
                && Objects.equals(reference, that.reference)
                && Objects.equals(pattern, that.pattern)
                && Objects.equals(oneOf, that.oneOf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, dimension, extensions, name, aliases, description, reference, pattern, oneOf);
    }

    @Override
    public String toString() {
        return "Property{" + "type="
                + type + ", dimension="
                + dimension + ", extensions="
                + extensions + ", name="
                + name + ", aliases="
                + aliases + ", description="
                + description + ", reference="
                + reference + ", pattern="
                + pattern + ", oneOf="
                + oneOf + "}";
    }

    /** Builder for {@link Property}. */
    public static final class Builder {
        private Neo4jType type;
        private Integer dimension;
        private PropertyExtensions extensions;
        private String name;
        private final List<String> aliases = new ArrayList<>();
        private String description;
        private String reference;
        private String pattern;
        private final List<String> oneOf = new ArrayList<>();

        private Builder() {}

        private Builder(Property v) {
            this.type = v.type;
            this.dimension = v.dimension;
            this.extensions = v.extensions;
            this.name = v.name;
            this.aliases.addAll(v.aliases);
            this.description = v.description;
            this.reference = v.reference;
            this.pattern = v.pattern;
            this.oneOf.addAll(v.oneOf);
        }

        /**
         * Sets neo4j type of the property.
         *
         * @param type the value, or null to clear it
         * @return this builder
         */
        public Builder type(Neo4jType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets dimension of a vector typed property.
         *
         * @param dimension the value, or null to clear it
         * @return this builder
         */
        public Builder dimension(Integer dimension) {
            this.dimension = dimension;
            return this;
        }

        /**
         * Sets extensions for the property.
         *
         * @param extensions the value, or null to clear it
         * @return this builder
         */
        public Builder extensions(PropertyExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Sets name of the property in the graph, when different from its id.
         *
         * @param name the value, or null to clear it
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Replaces alternative names for the property.
         *
         * @param aliases the values, or null to clear
         * @return this builder
         */
        public Builder aliases(List<String> aliases) {
            this.aliases.clear();
            if (aliases != null) {
                this.aliases.addAll(aliases);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder alias(String value) {
            this.aliases.add(Objects.requireNonNull(value, "alias"));
            return this;
        }

        /**
         * Sets description of the property.
         *
         * @param description the value, or null to clear it
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets reference or URI of an external resource describing the property.
         *
         * @param reference the value, or null to clear it
         * @return this builder
         */
        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Sets regular expression values must match.
         *
         * @param pattern the value, or null to clear it
         * @return this builder
         */
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Replaces values the property is restricted to (wire key {@code one_of}).
         *
         * @param oneOf the values, or null to clear
         * @return this builder
         */
        public Builder oneOf(List<String> oneOf) {
            this.oneOf.clear();
            if (oneOf != null) {
                this.oneOf.addAll(oneOf);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder oneOfValue(String value) {
            this.oneOf.add(Objects.requireNonNull(value, "oneOfValue"));
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public Property build() {
            return new Property(this);
        }
    }
}
