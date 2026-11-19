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

/** A constraint on a relationship. */
public final class RelationshipConstraint {
    private final ConstraintType type;
    private final List<String> properties;
    private final RelationshipConstraintExtensions extensions;
    private final String name;

    private RelationshipConstraint(Builder b) {
        this.type = b.type;
        this.properties = Collections.unmodifiableList(new ArrayList<>(b.properties));
        this.extensions = b.extensions;
        this.name = b.name;
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

    /** @return kind of constraint. */
    public ConstraintType type() {
        return type;
    }

    /** @return ids of the properties the constraint applies to. */
    public List<String> properties() {
        return properties;
    }

    /** @return extensions for the constraint. */
    public Optional<RelationshipConstraintExtensions> extensions() {
        return Optional.ofNullable(extensions);
    }

    /** @return name of the constraint in the database. */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RelationshipConstraint that = (RelationshipConstraint) o;
        return Objects.equals(type, that.type)
                && Objects.equals(properties, that.properties)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, properties, extensions, name);
    }

    @Override
    public String toString() {
        return "RelationshipConstraint{" + "type="
                + type + ", properties="
                + properties + ", extensions="
                + extensions + ", name="
                + name + "}";
    }

    /** Builder for {@link RelationshipConstraint}. */
    public static final class Builder {
        private ConstraintType type;
        private final List<String> properties = new ArrayList<>();
        private RelationshipConstraintExtensions extensions;
        private String name;

        private Builder() {}

        private Builder(RelationshipConstraint v) {
            this.type = v.type;
            this.properties.addAll(v.properties);
            this.extensions = v.extensions;
            this.name = v.name;
        }

        /**
         * Sets kind of constraint.
         *
         * @param type the value, or null to clear it
         * @return this builder
         */
        public Builder type(ConstraintType type) {
            this.type = type;
            return this;
        }

        /**
         * Replaces ids of the properties the constraint applies to.
         *
         * @param properties the values, or null to clear
         * @return this builder
         */
        public Builder properties(List<String> properties) {
            this.properties.clear();
            if (properties != null) {
                this.properties.addAll(properties);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder property(String value) {
            this.properties.add(Objects.requireNonNull(value, "property"));
            return this;
        }

        /**
         * Sets extensions for the constraint.
         *
         * @param extensions the value, or null to clear it
         * @return this builder
         */
        public Builder extensions(RelationshipConstraintExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Sets name of the constraint in the database.
         *
         * @param name the value, or null to clear it
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public RelationshipConstraint build() {
            if (type == null) {
                throw new IllegalStateException("RelationshipConstraint: required field 'type' is missing");
            }
            return new RelationshipConstraint(this);
        }
    }
}
