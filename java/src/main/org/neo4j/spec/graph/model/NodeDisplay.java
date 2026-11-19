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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Visual information for a node. */
public final class NodeDisplay {
    private final Double x;
    private final Double y;
    private final String colour;
    private final Map<String, Object> custom;

    private NodeDisplay(Builder b) {
        this.x = b.x;
        this.y = b.y;
        this.colour = b.colour;
        this.custom = Values.deepUnmodifiable(b.custom);
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

    /** @return horizontal position. */
    public Double x() {
        return x;
    }

    /** @return vertical position. */
    public Double y() {
        return y;
    }

    /** @return display colour of the node as a hex string, e.g. #FF8800. */
    public Optional<String> colour() {
        return Optional.ofNullable(colour);
    }

    /** @return user-defined arbitrary data. */
    public Map<String, Object> custom() {
        return custom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeDisplay that = (NodeDisplay) o;
        return Objects.equals(x, that.x)
                && Objects.equals(y, that.y)
                && Objects.equals(colour, that.colour)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, colour, custom);
    }

    @Override
    public String toString() {
        return "NodeDisplay{" + "x=" + x + ", y=" + y + ", colour=" + colour + ", custom=" + custom + "}";
    }

    /** Builder for {@link NodeDisplay}. */
    public static final class Builder {
        private Double x;
        private Double y;
        private String colour;
        private final Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        private Builder(NodeDisplay v) {
            this.x = v.x;
            this.y = v.y;
            this.colour = v.colour;
            this.custom.putAll(v.custom);
        }

        /**
         * Sets horizontal position.
         *
         * @param x the value, or null to clear it
         * @return this builder
         */
        public Builder x(Double x) {
            this.x = x;
            return this;
        }

        /**
         * Sets vertical position.
         *
         * @param y the value, or null to clear it
         * @return this builder
         */
        public Builder y(Double y) {
            this.y = y;
            return this;
        }

        /**
         * Sets display colour of the node as a hex string, e.g. #FF8800.
         *
         * @param colour the value, or null to clear it
         * @return this builder
         */
        public Builder colour(String colour) {
            this.colour = colour;
            return this;
        }

        /**
         * Replaces user-defined arbitrary data.
         *
         * @param custom the entries, or null to clear
         * @return this builder
         */
        public Builder custom(Map<String, Object> custom) {
            this.custom.clear();
            if (custom != null) {
                this.custom.putAll(custom);
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
        public Builder custom(String key, Object value) {
            this.custom.put(Objects.requireNonNull(key, "key"), value);
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public NodeDisplay build() {
            if (x == null) {
                throw new IllegalStateException("NodeDisplay: required field 'x' is missing");
            }
            if (y == null) {
                throw new IllegalStateException("NodeDisplay: required field 'y' is missing");
            }
            return new NodeDisplay(this);
        }
    }
}
