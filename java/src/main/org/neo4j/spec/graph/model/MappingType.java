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

import java.util.Optional;

/** The kind of a data source mapping. */
public enum MappingType {
    /** Wire value {@code NODE}. */
    NODE("NODE"),
    /** Wire value {@code RELATIONSHIP}. */
    RELATIONSHIP("RELATIONSHIP"),
    /** Wire value {@code QUERY}. */
    QUERY("QUERY");

    private final String value;

    MappingType(String value) {
        this.value = value;
    }

    /**
     * Returns the value used in documents.
     *
     * @return the wire value
     */
    public String value() {
        return value;
    }

    /**
     * Finds the constant with a wire value.
     *
     * @param value the wire value
     * @return the constant, or empty if unknown
     */
    public static Optional<MappingType> fromValue(String value) {
        for (MappingType c : values()) {
            if (c.value.equals(value)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
