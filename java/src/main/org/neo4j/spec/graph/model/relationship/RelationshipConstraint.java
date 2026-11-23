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
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.node.Constraint;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipConstraintExtensions;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.Named;

@JsonClassDescription("A constraint on a relationship.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class RelationshipConstraint implements Constraint, Named {
    private ConstraintType type;

    private final List<String> properties;

    private final RelationshipConstraintExtensions extensions;

    private String name;

    private RelationshipConstraint(
            ConstraintType type, List<String> properties, RelationshipConstraintExtensions extensions, String name) {
        this.type = type;
        this.properties = properties;
        this.extensions = extensions;
        this.name = name;
    }

    public static Builder builder(ConstraintType type, List<String> properties) {
        return new Builder(type, properties);
    }

    @Override
    public ConstraintType getType() {
        return this.type;
    }

    public void setType(ConstraintType value) {
        this.type = value;
    }

    @Override
    public List<String> getProperties() {
        return this.properties;
    }

    public RelationshipConstraintExtensions getExtensions() {
        return this.extensions;
    }

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
        if (!(other instanceof RelationshipConstraint)) return false;
        RelationshipConstraint that = (RelationshipConstraint) other;
        return Objects.equals(type, that.type)
                && Objects.equals(properties, that.properties)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(properties);
        result = 31 * result + Objects.hashCode(extensions);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }

    @Override
    public String toString() {
        return "RelationshipConstraint(type=" + type + ", properties=" + properties + ", extensions=" + extensions
                + ", name=" + name + ")";
    }

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the constraint.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private ConstraintType jsonType() {
        return this.type;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Ids of the properties the constraint covers.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<String> jsonProperties() {
        return this.properties;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions for the constraint.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RelationshipConstraintExtensions jsonExtensions() {
        return Objects.equals(
                        this.extensions,
                        RelationshipConstraintExtensions.builder().build())
                ? null
                : this.extensions;
    }

    @JsonProperty("name")
    @JsonPropertyDescription("Name of the constraint in the database.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonName() {
        return Objects.equals(this.name, null) ? null : this.name;
    }

    @JsonCreator
    private static RelationshipConstraint fromJson(
            @JsonProperty("type") ConstraintType type,
            @JsonProperty("properties") List<String> properties,
            @JsonProperty("extensions") RelationshipConstraintExtensions extensions,
            @JsonProperty("name") String name) {
        return new RelationshipConstraint(
                Objects.requireNonNull(type, "Field 'type' is required"),
                Objects.requireNonNull(properties, "Field 'properties' is required"),
                extensions != null
                        ? extensions
                        : RelationshipConstraintExtensions.builder().build(),
                name != null ? name : null);
    }

    public static final class Builder {
        private ConstraintType type;
        private List<String> properties;
        private RelationshipConstraintExtensions extensions =
                RelationshipConstraintExtensions.builder().build();
        private String name = null;

        private Builder(ConstraintType type, List<String> properties) {
            this.type = type;
            this.properties = properties;
        }

        public Builder type(ConstraintType type) {
            this.type = type;
            return this;
        }

        public Builder properties(List<String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder extensions(RelationshipConstraintExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public RelationshipConstraint build() {
            return new RelationshipConstraint(type, properties, extensions, name);
        }
    }
}
