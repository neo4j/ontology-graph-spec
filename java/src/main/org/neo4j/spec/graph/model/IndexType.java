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

/** The kind of an index. */
public enum IndexType {
    /** Wire value {@code FULLTEXT}. */
    FULLTEXT("FULLTEXT"),
    /** Wire value {@code POINT}. */
    POINT("POINT"),
    /** Wire value {@code RANGE}. */
    RANGE("RANGE"),
    /** Wire value {@code TEXT}. */
    TEXT("TEXT"),
    /** Wire value {@code VECTOR}. */
    VECTOR("VECTOR"),
    /** Wire value {@code LOOKUP}. */
    LOOKUP("LOOKUP");

    private final String value;

    IndexType(String value) {
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
    public static Optional<IndexType> fromValue(String value) {
        for (IndexType c : values()) {
            if (c.value.equals(value)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
