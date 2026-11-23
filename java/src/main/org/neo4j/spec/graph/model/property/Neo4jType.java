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
package org.neo4j.spec.graph.model.property;

import com.fasterxml.jackson.annotation.JsonValue;

/** A Neo4j property type. */
public enum Neo4jType {
    ANY("ANY"),
    LIST_ANY("LIST<ANY>"),
    BOOLEAN("BOOLEAN"),
    LIST_BOOLEAN("LIST<BOOLEAN>"),
    DATE("DATE"),
    LIST_DATE("LIST<DATE>"),
    DURATION("DURATION"),
    LIST_DURATION("LIST<DURATION>"),
    FLOAT32("FLOAT32"),
    LIST_FLOAT32("LIST<FLOAT32>"),
    FLOAT("FLOAT"),
    LIST_FLOAT("LIST<FLOAT>"),
    INTEGER8("INTEGER8"),
    LIST_INTEGER8("LIST<INTEGER8>"),
    INTEGER16("INTEGER16"),
    LIST_INTEGER16("LIST<INTEGER16>"),
    INTEGER32("INTEGER32"),
    LIST_INTEGER32("LIST<INTEGER32>"),
    INTEGER("INTEGER"),
    LIST_INTEGER("LIST<INTEGER>"),
    LOCAL_DATETIME("LOCAL DATETIME"),
    LIST_LOCAL_DATETIME("LIST<LOCAL DATETIME>"),
    LOCAL_TIME("LOCAL TIME"),
    LIST_LOCAL_TIME("LIST<LOCAL TIME>"),
    POINT("POINT"),
    LIST_POINT("LIST<POINT>"),
    STRING("STRING"),
    LIST_STRING("LIST<STRING>"),
    VECTOR_FLOAT("VECTOR<FLOAT>"),
    VECTOR_FLOAT32("VECTOR<FLOAT32>"),
    VECTOR_INTEGER("VECTOR<INTEGER>"),
    VECTOR_INTEGER32("VECTOR<INTEGER32>"),
    VECTOR_INTEGER16("VECTOR<INTEGER16>"),
    VECTOR_INTEGER8("VECTOR<INTEGER8>"),
    ZONED_DATETIME("ZONED DATETIME"),
    LIST_ZONED_DATETIME("LIST<ZONED DATETIME>"),
    ZONED_TIME("ZONED TIME"),
    LIST_ZONED_TIME("LIST<ZONED TIME>"),
    UUID("UUID");

    private final String value;

    Neo4jType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static Neo4jType fromString(String string) {
        for (Neo4jType type : values()) {
            if (type.value.equals(string)) {
                return type;
            }
        }
        return null;
    }

    public static String toString(Neo4jType type) {
        return type.value;
    }

    public static boolean isVector(Neo4jType type) {
        return type == VECTOR_FLOAT
                || type == VECTOR_FLOAT32
                || type == VECTOR_INTEGER
                || type == VECTOR_INTEGER32
                || type == VECTOR_INTEGER16
                || type == VECTOR_INTEGER8;
    }
}
