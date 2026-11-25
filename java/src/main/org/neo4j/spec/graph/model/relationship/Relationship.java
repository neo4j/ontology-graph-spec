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
package org.neo4j.spec.graph.model.relationship;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipExtensions;

/** A relationship of the graph. */
public final class Relationship {
    private String type;

    private final RelationshipTarget from;

    private final RelationshipTarget to;

    private final Map<String, Property> properties;

    private final Map<String, RelationshipConstraint> constraints;

    private final RelationshipExtensions extensions;

    private final String description;

    private final List<String> aliases;

    private final String reference;

    private Relationship(Builder builder) {
        this.type = builder.type;
        this.from = builder.from;
        this.to = builder.to;
        this.properties = new LinkedHashMap<>(builder.properties);
        this.constraints = new LinkedHashMap<>(builder.constraints);
        this.extensions = builder.extensions;
        this.description = builder.description;
        this.aliases = builder.aliases;
        this.reference = builder.reference;
    }

    public static Builder builder(String type, RelationshipTarget from, RelationshipTarget to) {
        return new Builder(type, from, to);
    }

    /** Type of the relationship. */
    public String getType() {
        return this.type;
    }

    public void setType(String value) {
        this.type = value;
    }

    /** The node the relationship starts at. */
    public RelationshipTarget getFrom() {
        return this.from;
    }

    /** The node the relationship ends at. */
    public RelationshipTarget getTo() {
        return this.to;
    }

    /** Properties of the relationship keyed by id. */
    public Map<String, Property> getProperties() {
        return this.properties;
    }

    /** Constraints on the relationship keyed by id. */
    public Map<String, RelationshipConstraint> getConstraints() {
        return this.constraints;
    }

    /** Extensions for the relationship. */
    public RelationshipExtensions getExtensions() {
        return this.extensions;
    }

    /** Description of the relationship. */
    public String getDescription() {
        return this.description;
    }

    /** Alternative names for the relationship. */
    public List<String> getAliases() {
        return this.aliases;
    }

    /** Reference or URI of an external resource describing the relationship. */
    public String getReference() {
        return this.reference;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Relationship)) return false;
        Relationship that = (Relationship) other;
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
        return "Relationship(type=" + type + ", from=" + from + ", to=" + to + ", properties=" + properties
                + ", constraints=" + constraints + ", extensions=" + extensions + ", description=" + description
                + ", aliases=" + aliases + ", reference=" + reference + ")";
    }

    public static final class Builder {
        private String type;
        private RelationshipTarget from;
        private RelationshipTarget to;
        private Map<String, Property> properties = new LinkedHashMap<>();
        private Map<String, RelationshipConstraint> constraints = new LinkedHashMap<>();
        private RelationshipExtensions extensions =
                RelationshipExtensions.builder().build();
        private String description = "";
        private List<String> aliases = new ArrayList<>();
        private String reference = "";

        private Builder(String type, RelationshipTarget from, RelationshipTarget to) {
            this.type = type;
            this.from = from;
            this.to = to;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder from(RelationshipTarget from) {
            this.from = from;
            return this;
        }

        public Builder to(RelationshipTarget to) {
            this.to = to;
            return this;
        }

        public Builder properties(Map<String, Property> properties) {
            this.properties = properties;
            return this;
        }

        public Builder constraints(Map<String, RelationshipConstraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder extensions(RelationshipExtensions extensions) {
            this.extensions = extensions;
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

        public Relationship build() {
            Objects.requireNonNull(type, "Field 'type' is required");
            Objects.requireNonNull(from, "Field 'from' is required");
            Objects.requireNonNull(to, "Field 'to' is required");
            return new Relationship(this);
        }
    }
}
