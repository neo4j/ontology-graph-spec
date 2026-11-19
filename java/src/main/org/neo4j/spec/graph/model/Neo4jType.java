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

/** A Neo4j property type. */
public enum Neo4jType {
    /** Wire value {@code ANY}. */
    ANY("ANY"),
    /** Wire value {@code LIST<ANY>}. */
    LIST_ANY("LIST<ANY>"),
    /** Wire value {@code BOOLEAN}. */
    BOOLEAN("BOOLEAN"),
    /** Wire value {@code LIST<BOOLEAN>}. */
    LIST_BOOLEAN("LIST<BOOLEAN>"),
    /** Wire value {@code DATE}. */
    DATE("DATE"),
    /** Wire value {@code LIST<DATE>}. */
    LIST_DATE("LIST<DATE>"),
    /** Wire value {@code DURATION}. */
    DURATION("DURATION"),
    /** Wire value {@code LIST<DURATION>}. */
    LIST_DURATION("LIST<DURATION>"),
    /** Wire value {@code FLOAT32}. */
    FLOAT32("FLOAT32"),
    /** Wire value {@code LIST<FLOAT32>}. */
    LIST_FLOAT32("LIST<FLOAT32>"),
    /** Wire value {@code FLOAT}. */
    FLOAT("FLOAT"),
    /** Wire value {@code LIST<FLOAT>}. */
    LIST_FLOAT("LIST<FLOAT>"),
    /** Wire value {@code INTEGER8}. */
    INTEGER8("INTEGER8"),
    /** Wire value {@code LIST<INTEGER8>}. */
    LIST_INTEGER8("LIST<INTEGER8>"),
    /** Wire value {@code INTEGER16}. */
    INTEGER16("INTEGER16"),
    /** Wire value {@code LIST<INTEGER16>}. */
    LIST_INTEGER16("LIST<INTEGER16>"),
    /** Wire value {@code INTEGER32}. */
    INTEGER32("INTEGER32"),
    /** Wire value {@code LIST<INTEGER32>}. */
    LIST_INTEGER32("LIST<INTEGER32>"),
    /** Wire value {@code INTEGER}. */
    INTEGER("INTEGER"),
    /** Wire value {@code LIST<INTEGER>}. */
    LIST_INTEGER("LIST<INTEGER>"),
    /** Wire value {@code LOCAL DATETIME}. */
    LOCAL_DATETIME("LOCAL DATETIME"),
    /** Wire value {@code LIST<LOCAL DATETIME>}. */
    LIST_LOCAL_DATETIME("LIST<LOCAL DATETIME>"),
    /** Wire value {@code LOCAL TIME}. */
    LOCAL_TIME("LOCAL TIME"),
    /** Wire value {@code LIST<LOCAL TIME>}. */
    LIST_LOCAL_TIME("LIST<LOCAL TIME>"),
    /** Wire value {@code POINT}. */
    POINT("POINT"),
    /** Wire value {@code LIST<POINT>}. */
    LIST_POINT("LIST<POINT>"),
    /** Wire value {@code STRING}. */
    STRING("STRING"),
    /** Wire value {@code LIST<STRING>}. */
    LIST_STRING("LIST<STRING>"),
    /** Wire value {@code VECTOR<FLOAT>}. */
    VECTOR_FLOAT("VECTOR<FLOAT>"),
    /** Wire value {@code VECTOR<FLOAT32>}. */
    VECTOR_FLOAT32("VECTOR<FLOAT32>"),
    /** Wire value {@code VECTOR<INTEGER>}. */
    VECTOR_INTEGER("VECTOR<INTEGER>"),
    /** Wire value {@code VECTOR<INTEGER32>}. */
    VECTOR_INTEGER32("VECTOR<INTEGER32>"),
    /** Wire value {@code VECTOR<INTEGER16>}. */
    VECTOR_INTEGER16("VECTOR<INTEGER16>"),
    /** Wire value {@code VECTOR<INTEGER8>}. */
    VECTOR_INTEGER8("VECTOR<INTEGER8>"),
    /** Wire value {@code ZONED DATETIME}. */
    ZONED_DATETIME("ZONED DATETIME"),
    /** Wire value {@code LIST<ZONED DATETIME>}. */
    LIST_ZONED_DATETIME("LIST<ZONED DATETIME>"),
    /** Wire value {@code ZONED TIME}. */
    ZONED_TIME("ZONED TIME"),
    /** Wire value {@code LIST<ZONED TIME>}. */
    LIST_ZONED_TIME("LIST<ZONED TIME>"),
    /** Wire value {@code UUID}. */
    UUID("UUID");

    private final String value;

    Neo4jType(String value) {
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
    public static Optional<Neo4jType> fromValue(String value) {
        for (Neo4jType c : values()) {
            if (c.value.equals(value)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
