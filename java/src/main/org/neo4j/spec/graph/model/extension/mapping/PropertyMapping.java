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
package org.neo4j.spec.graph.model.extension.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Maps a property to a source column. */
public final class PropertyMapping {
    private String column;

    @JsonCreator
    public PropertyMapping(@JsonProperty("column") String column) {
        this.column = Objects.requireNonNull(column, "Field 'column' is required");
    }

    /** Name of the source column. */
    public String getColumn() {
        return this.column;
    }

    public void setColumn(String value) {
        this.column = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PropertyMapping)) return false;
        PropertyMapping that = (PropertyMapping) other;
        return Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column);
    }

    @Override
    public String toString() {
        return "PropertyMapping(column=" + column + ")";
    }
}
