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

/** The kind of a constraint. */
public enum ConstraintType {
    /** Wire value {@code EXISTS}. */
    EXISTS("EXISTS"),
    /** Wire value {@code KEY}. */
    KEY("KEY"),
    /** Wire value {@code PROPERTY_TYPE}. */
    PROPERTY_TYPE("PROPERTY_TYPE"),
    /** Wire value {@code UNIQUE}. */
    UNIQUE("UNIQUE");

    private final String value;

    ConstraintType(String value) {
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
    public static Optional<ConstraintType> fromValue(String value) {
        for (ConstraintType c : values()) {
            if (c.value.equals(value)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
