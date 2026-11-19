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

import java.util.Objects;

/** Maps a property to a source column. */
public final class PropertyMapping {
    private final String column;

    private PropertyMapping(Builder b) {
        this.column = b.column;
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

    /** @return name of the source column. */
    public String column() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyMapping that = (PropertyMapping) o;
        return Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column);
    }

    @Override
    public String toString() {
        return "PropertyMapping{" + "column=" + column + "}";
    }

    /** Builder for {@link PropertyMapping}. */
    public static final class Builder {
        private String column;

        private Builder() {}

        private Builder(PropertyMapping v) {
            this.column = v.column;
        }

        /**
         * Sets name of the source column.
         *
         * @param column the value, or null to clear it
         * @return this builder
         */
        public Builder column(String column) {
            this.column = column;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public PropertyMapping build() {
            if (column == null) {
                throw new IllegalStateException("PropertyMapping: required field 'column' is missing");
            }
            return new PropertyMapping(this);
        }
    }
}
