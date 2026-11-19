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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A relationship of the graph. */
public final class Relationship {
    private final String type;
    private final RelationshipTarget from;
    private final RelationshipTarget to;
    private final Map<String, Property> properties;
    private final Map<String, RelationshipConstraint> constraints;
    private final RelationshipExtensions extensions;
    private final String description;
    private final List<String> aliases;
    private final String reference;

    private Relationship(Builder b) {
        this.type = b.type;
        this.from = b.from;
        this.to = b.to;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(b.properties));
        this.constraints = Collections.unmodifiableMap(new LinkedHashMap<>(b.constraints));
        this.extensions = b.extensions;
        this.description = b.description;
        this.aliases = Collections.unmodifiableList(new ArrayList<>(b.aliases));
        this.reference = b.reference;
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

    /** @return relationship type in the graph. */
    public String type() {
        return type;
    }

    /** @return start of the relationship. */
    public RelationshipTarget from() {
        return from;
    }

    /** @return end of the relationship. */
    public RelationshipTarget to() {
        return to;
    }

    /** @return properties of the relationship keyed by id. */
    public Map<String, Property> properties() {
        return properties;
    }

    /** @return constraints on the relationship keyed by id. */
    public Map<String, RelationshipConstraint> constraints() {
        return constraints;
    }

    /** @return extensions for the relationship. */
    public Optional<RelationshipExtensions> extensions() {
        return Optional.ofNullable(extensions);
    }

    /** @return description of the relationship. */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /** @return alternative names for the relationship. */
    public List<String> aliases() {
        return aliases;
    }

    /** @return reference or URI of an external resource describing the relationship. */
    public Optional<String> reference() {
        return Optional.ofNullable(reference);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Relationship that = (Relationship) o;
        return Objects.equals(type, that.type)
                && Objects.equals(from, that.from)
                && Objects.equals(to, that.to)
                && Objects.equals(properties, that.properties)
                && Objects.equals(constraints, that.constraints)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(description, that.description)
                && Objects.equals(aliases, that.aliases)
                && Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, from, to, properties, constraints, extensions, description, aliases, reference);
    }

    @Override
    public String toString() {
        return "Relationship{" + "type="
                + type + ", from="
                + from + ", to="
                + to + ", properties="
                + properties + ", constraints="
                + constraints + ", extensions="
                + extensions + ", description="
                + description + ", aliases="
                + aliases + ", reference="
                + reference + "}";
    }

    /** Builder for {@link Relationship}. */
    public static final class Builder {
        private String type;
        private RelationshipTarget from;
        private RelationshipTarget to;
        private final Map<String, Property> properties = new LinkedHashMap<>();
        private final Map<String, RelationshipConstraint> constraints = new LinkedHashMap<>();
        private RelationshipExtensions extensions;
        private String description;
        private final List<String> aliases = new ArrayList<>();
        private String reference;

        private Builder() {}

        private Builder(Relationship v) {
            this.type = v.type;
            this.from = v.from;
            this.to = v.to;
            this.properties.putAll(v.properties);
            this.constraints.putAll(v.constraints);
            this.extensions = v.extensions;
            this.description = v.description;
            this.aliases.addAll(v.aliases);
            this.reference = v.reference;
        }

        /**
         * Sets relationship type in the graph.
         *
         * @param type the value, or null to clear it
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets start of the relationship.
         *
         * @param from the value, or null to clear it
         * @return this builder
         */
        public Builder from(RelationshipTarget from) {
            this.from = from;
            return this;
        }

        /**
         * Sets end of the relationship.
         *
         * @param to the value, or null to clear it
         * @return this builder
         */
        public Builder to(RelationshipTarget to) {
            this.to = to;
            return this;
        }

        /**
         * Replaces properties of the relationship keyed by id.
         *
         * @param properties the entries, or null to clear
         * @return this builder
         */
        public Builder properties(Map<String, Property> properties) {
            this.properties.clear();
            if (properties != null) {
                this.properties.putAll(properties);
            }
            return this;
        }

        /**
         * Adds or replaces one entry.
         *
         * @param key the entry key
         * @param value the entry value
         * @return this builder
         */
        public Builder property(String key, Property value) {
            this.properties.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Replaces constraints on the relationship keyed by id.
         *
         * @param constraints the entries, or null to clear
         * @return this builder
         */
        public Builder constraints(Map<String, RelationshipConstraint> constraints) {
            this.constraints.clear();
            if (constraints != null) {
                this.constraints.putAll(constraints);
            }
            return this;
        }

        /**
         * Adds or replaces one entry.
         *
         * @param key the entry key
         * @param value the entry value
         * @return this builder
         */
        public Builder constraint(String key, RelationshipConstraint value) {
            this.constraints.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Sets extensions for the relationship.
         *
         * @param extensions the value, or null to clear it
         * @return this builder
         */
        public Builder extensions(RelationshipExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Sets description of the relationship.
         *
         * @param description the value, or null to clear it
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Replaces alternative names for the relationship.
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
         * Sets reference or URI of an external resource describing the relationship.
         *
         * @param reference the value, or null to clear it
         * @return this builder
         */
        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public Relationship build() {
            if (type == null) {
                throw new IllegalStateException("Relationship: required field 'type' is missing");
            }
            if (from == null) {
                throw new IllegalStateException("Relationship: required field 'from' is missing");
            }
            if (to == null) {
                throw new IllegalStateException("Relationship: required field 'to' is missing");
            }
            return new Relationship(this);
        }
    }
}
