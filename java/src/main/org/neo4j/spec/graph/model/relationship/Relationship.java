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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipExtensions;

@JsonClassDescription("A relationship of the graph.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
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

    private Relationship(
            String type,
            RelationshipTarget from,
            RelationshipTarget to,
            Map<String, Property> properties,
            Map<String, RelationshipConstraint> constraints,
            RelationshipExtensions extensions,
            String description,
            List<String> aliases,
            String reference) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.properties = properties;
        this.constraints = constraints;
        this.extensions = extensions;
        this.description = description;
        this.aliases = aliases;
        this.reference = reference;
    }

    public static Builder builder(String type, RelationshipTarget from, RelationshipTarget to) {
        return new Builder(type, from, to);
    }

    public String getType() {
        return this.type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public RelationshipTarget getFrom() {
        return this.from;
    }

    public RelationshipTarget getTo() {
        return this.to;
    }

    public Map<String, Property> getProperties() {
        return this.properties;
    }

    public Map<String, RelationshipConstraint> getConstraints() {
        return this.constraints;
    }

    public RelationshipExtensions getExtensions() {
        return this.extensions;
    }

    public String getDescription() {
        return this.description;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

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

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the relationship.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonType() {
        return this.type;
    }

    @JsonProperty("from")
    @JsonPropertyDescription("The node the relationship starts at.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private RelationshipTarget jsonFrom() {
        return this.from;
    }

    @JsonProperty("to")
    @JsonPropertyDescription("The node the relationship ends at.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private RelationshipTarget jsonTo() {
        return this.to;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Properties of the relationship keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Property> jsonProperties() {
        return Objects.equals(this.properties, new LinkedHashMap<String, Property>()) ? null : this.properties;
    }

    @JsonProperty("constraints")
    @JsonPropertyDescription("Constraints on the relationship keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, RelationshipConstraint> jsonConstraints() {
        return Objects.equals(this.constraints, new LinkedHashMap<String, RelationshipConstraint>())
                ? null
                : this.constraints;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions for the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RelationshipExtensions jsonExtensions() {
        return Objects.equals(this.extensions, RelationshipExtensions.builder().build()) ? null : this.extensions;
    }

    @JsonProperty("description")
    @JsonPropertyDescription("Description of the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonDescription() {
        return Objects.equals(this.description, "") ? null : this.description;
    }

    @JsonProperty("aliases")
    @JsonPropertyDescription("Alternative names for the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonAliases() {
        return Objects.equals(this.aliases, new ArrayList<String>()) ? null : this.aliases;
    }

    @JsonProperty("reference")
    @JsonPropertyDescription("Reference or URI of an external resource describing the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonReference() {
        return Objects.equals(this.reference, "") ? null : this.reference;
    }

    @JsonCreator
    private static Relationship fromJson(
            @JsonProperty("type") String type,
            @JsonProperty("from") RelationshipTarget from,
            @JsonProperty("to") RelationshipTarget to,
            @JsonProperty("properties") Map<String, Property> properties,
            @JsonProperty("constraints") Map<String, RelationshipConstraint> constraints,
            @JsonProperty("extensions") RelationshipExtensions extensions,
            @JsonProperty("description") String description,
            @JsonProperty("aliases") List<String> aliases,
            @JsonProperty("reference") String reference) {
        return new Relationship(
                Objects.requireNonNull(type, "Field 'type' is required"),
                Objects.requireNonNull(from, "Field 'from' is required"),
                Objects.requireNonNull(to, "Field 'to' is required"),
                properties != null ? properties : new LinkedHashMap<>(),
                constraints != null ? constraints : new LinkedHashMap<>(),
                extensions != null
                        ? extensions
                        : RelationshipExtensions.builder().build(),
                description != null ? description : "",
                aliases != null ? aliases : new ArrayList<>(),
                reference != null ? reference : "");
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
            return new Relationship(
                    type, from, to, properties, constraints, extensions, description, aliases, reference);
        }
    }
}
