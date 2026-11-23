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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

@JsonClassDescription("Visual information for a node.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class NodeDisplay implements CustomExtensions {
    private double x;

    private double y;

    private String colour;

    private final Map<String, Object> custom;

    private NodeDisplay(double x, double y, String colour, Map<String, Object> custom) {
        this.x = x;
        this.y = y;
        this.colour = colour;
        this.custom = custom;
    }

    public static Builder builder(double x, double y) {
        return new Builder(x, y);
    }

    public double getX() {
        return this.x;
    }

    public void setX(double value) {
        this.x = value;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double value) {
        this.y = value;
    }

    public String getColour() {
        return this.colour;
    }

    public void setColour(String value) {
        this.colour = value;
    }

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
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Objects.hashCode(colour);
        result = 31 * result + Objects.hashCode(custom);
        return result;
    }

    @Override
    public String toString() {
        return "NodeDisplay(x=" + x + ", y=" + y + ", colour=" + colour + ", custom=" + custom + ")";
    }

    @JsonProperty("x")
    @JsonPropertyDescription("Horizontal position.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Double jsonX() {
        return this.x;
    }

    @JsonProperty("y")
    @JsonPropertyDescription("Vertical position.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Double jsonY() {
        return this.y;
    }

    @JsonProperty("colour")
    @JsonPropertyDescription("Display colour of the node as a hex string, e.g. #FF8800.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonColour() {
        return Objects.equals(this.colour, null) ? null : this.colour;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static NodeDisplay fromJson(
            @JsonProperty("x") Double x,
            @JsonProperty("y") Double y,
            @JsonProperty("colour") String colour,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new NodeDisplay(
                Objects.requireNonNull(x, "Field 'x' is required"),
                Objects.requireNonNull(y, "Field 'y' is required"),
                colour,
                custom != null ? custom : new LinkedHashMap<>());
    }

    public static final class Builder {
        private double x;
        private double y;
        private String colour = null;
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder(double x, double y) {
            this.x = x;
            this.y = y;
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
            return new NodeDisplay(x, y, colour, custom);
        }
    }
}
