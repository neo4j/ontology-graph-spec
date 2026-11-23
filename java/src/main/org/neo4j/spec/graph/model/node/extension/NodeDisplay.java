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
package org.neo4j.spec.graph.model.node.extension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

/** Visual information for a node. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = NodeDisplay.Builder.class)
public final class NodeDisplay implements CustomExtensions {
    private double x;

    private double y;

    private String colour;

    private final Map<String, Object> custom;

    /** Default instance Jackson compares against to omit default values. */
    private NodeDisplay() {
        this(new Builder(0.0, 0.0));
    }

    private NodeDisplay(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.colour = builder.colour;
        this.custom = builder.custom;
    }

    public static Builder builder(double x, double y) {
        return new Builder(x, y);
    }

    /** Horizontal position. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public double getX() {
        return this.x;
    }

    public void setX(double value) {
        this.x = value;
    }

    /** Vertical position. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public double getY() {
        return this.y;
    }

    public void setY(double value) {
        this.y = value;
    }

    /** Display colour of the node as a hex string, e.g. #FF8800. */
    public String getColour() {
        return this.colour;
    }

    public void setColour(String value) {
        this.colour = value;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NodeDisplay)) return false;
        NodeDisplay that = (NodeDisplay) other;
        return Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Objects.equals(colour, that.colour)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, colour, custom);
    }

    @Override
    public String toString() {
        return "NodeDisplay(x=" + x + ", y=" + y + ", colour=" + colour + ", custom=" + custom + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private double x;
        private double y;
        private String colour = null;
        private Map<String, Object> custom = new LinkedHashMap<>();

        @JsonCreator
        private Builder(@JsonProperty("x") Double x, @JsonProperty("y") Double y) {
            this.x = Objects.requireNonNull(x, "Field 'x' is required");
            this.y = Objects.requireNonNull(y, "Field 'y' is required");
        }

        public Builder x(double x) {
            this.x = x;
            return this;
        }

        public Builder y(double y) {
            this.y = y;
            return this;
        }

        public Builder colour(String colour) {
            this.colour = colour;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public NodeDisplay build() {
            return new NodeDisplay(this);
        }
    }
}
