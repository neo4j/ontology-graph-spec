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
package org.neo4j.spec.graph.model.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.node.extension.NodeConstraintExtensions;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.Named;

/** A constraint on a node. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = NodeConstraint.Builder.class)
public final class NodeConstraint implements Constraint, Named {
    private ConstraintType type;

    private final List<String> properties;

    private final NodeConstraintExtensions extensions;

    private String name;

    /** Default instance Jackson compares against to omit default values. */
    private NodeConstraint() {
        this(new Builder(null, null));
    }

    private NodeConstraint(Builder builder) {
        this.type = builder.type;
        this.properties = builder.properties;
        this.extensions = builder.extensions;
        this.name = builder.name;
    }

    public static Builder builder(ConstraintType type, List<String> properties) {
        return new Builder(type, properties);
    }

    /** Type of the constraint. */
    @Override
    public ConstraintType getType() {
        return this.type;
    }

    public void setType(ConstraintType value) {
        this.type = value;
    }

    /** Ids of the properties the constraint covers. */
    @Override
    public List<String> getProperties() {
        return this.properties;
    }

    /** Extensions for the constraint. */
    public NodeConstraintExtensions getExtensions() {
        return this.extensions;
    }

    /** Name of the constraint in the database. */
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
        if (!(other instanceof NodeConstraint)) return false;
        NodeConstraint that = (NodeConstraint) other;
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
        return "NodeConstraint(type=" + type + ", properties=" + properties + ", extensions=" + extensions + ", name="
                + name + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private ConstraintType type;
        private List<String> properties;
        private NodeConstraintExtensions extensions =
                NodeConstraintExtensions.builder().build();
        private String name = null;

        @JsonCreator
        private Builder(
                @JsonProperty("type") ConstraintType type, @JsonProperty("properties") List<String> properties) {
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

        public Builder extensions(NodeConstraintExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public NodeConstraint build() {
            Objects.requireNonNull(type, "Field 'type' is required");
            Objects.requireNonNull(properties, "Field 'properties' is required");
            return new NodeConstraint(this);
        }
    }
}
