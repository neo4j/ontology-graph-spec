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
package org.neo4j.spec.graph.model.property

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmStatic

@JsExport
@Serializable
@SerialName("Neo4jType")
@Description("A Neo4j property type.")
enum class Neo4jType {
    ANY,

    @SerialName("LIST<ANY>")
    LIST_ANY,

    BOOLEAN,

    @SerialName("LIST<BOOLEAN>")
    LIST_BOOLEAN,
    DATE,

    @SerialName("LIST<DATE>")
    LIST_DATE,
    DURATION,

    @SerialName("LIST<DURATION>")
    LIST_DURATION,
    FLOAT32,

    @SerialName("LIST<FLOAT32>")
    LIST_FLOAT32,
    FLOAT,

    @SerialName("LIST<FLOAT>")
    LIST_FLOAT,
    INTEGER8,

    @SerialName("LIST<INTEGER8>")
    LIST_INTEGER8,
    INTEGER16,

    @SerialName("LIST<INTEGER16>")
    LIST_INTEGER16,
    INTEGER32,

    @SerialName("LIST<INTEGER32>")
    LIST_INTEGER32,

    @SerialName("INTEGER")
    INTEGER,

    @SerialName("LIST<INTEGER>")
    LIST_INTEGER,

    @SerialName("LOCAL DATETIME")
    LOCAL_DATETIME,

    @SerialName("LIST<LOCAL DATETIME>")
    LIST_LOCAL_DATETIME,

    @SerialName("LOCAL TIME")
    LOCAL_TIME,

    @SerialName("LIST<LOCAL TIME>")
    LIST_LOCAL_TIME,
    POINT,

    @SerialName("LIST<POINT>")
    LIST_POINT,
    STRING,

    @SerialName("LIST<STRING>")
    LIST_STRING,

    @SerialName("VECTOR<FLOAT>")
    VECTOR_FLOAT,

    @SerialName("VECTOR<FLOAT32>")
    VECTOR_FLOAT32,

    @SerialName("VECTOR<INTEGER>")
    VECTOR_INTEGER,

    @SerialName("VECTOR<INTEGER32>")
    VECTOR_INTEGER32,

    @SerialName("VECTOR<INTEGER16>")
    VECTOR_INTEGER16,

    @SerialName("VECTOR<INTEGER8>")
    VECTOR_INTEGER8,

    @SerialName("ZONED DATETIME")
    ZONED_DATETIME,

    @SerialName("LIST<ZONED DATETIME>")
    LIST_ZONED_DATETIME,

    @SerialName("ZONED TIME")
    ZONED_TIME,

    @SerialName("LIST<ZONED TIME>")
    LIST_ZONED_TIME,

    UUID;

    companion object {
        // Ideally we'd have these as values inside the enum however
        // this would break the js union conversion
        @JvmStatic
        fun fromString(string: String): Neo4jType? = when (string) {
            "ANY" -> ANY
            "LIST<ANY>" -> LIST_ANY
            "BOOLEAN" -> BOOLEAN
            "LIST<BOOLEAN>" -> LIST_BOOLEAN
            "DATE" -> DATE
            "LIST<DATE>" -> LIST_DATE
            "DURATION" -> DURATION
            "LIST<DURATION>" -> LIST_DURATION
            "FLOAT32" -> FLOAT32
            "LIST<FLOAT32>" -> LIST_FLOAT32
            "FLOAT" -> FLOAT
            "LIST<FLOAT>" -> LIST_FLOAT
            "INTEGER8" -> INTEGER8
            "LIST<INTEGER8>" -> LIST_INTEGER8
            "INTEGER16" -> INTEGER16
            "LIST<INTEGER16>" -> LIST_INTEGER16
            "INTEGER32" -> INTEGER32
            "LIST<INTEGER32>" -> LIST_INTEGER32
            "INTEGER" -> INTEGER
            "LIST<INTEGER>" -> LIST_INTEGER
            "LOCAL DATETIME" -> LOCAL_DATETIME
            "LIST<LOCAL DATETIME>" -> LIST_LOCAL_DATETIME
            "LOCAL TIME" -> LOCAL_TIME
            "LIST<LOCAL TIME>" -> LIST_LOCAL_TIME
            "POINT" -> POINT
            "LIST<POINT>" -> LIST_POINT
            "STRING" -> STRING
            "LIST<STRING>" -> LIST_STRING
            "VECTOR<FLOAT>" -> VECTOR_FLOAT
            "VECTOR<FLOAT32>" -> VECTOR_FLOAT32
            "VECTOR<INTEGER>" -> VECTOR_INTEGER
            "VECTOR<INTEGER32>" -> VECTOR_INTEGER32
            "VECTOR<INTEGER16>" -> VECTOR_INTEGER16
            "VECTOR<INTEGER8>" -> VECTOR_INTEGER8
            "ZONED DATETIME" -> ZONED_DATETIME
            "LIST<ZONED DATETIME>" -> LIST_ZONED_DATETIME
            "ZONED TIME" -> ZONED_TIME
            "LIST<ZONED TIME>" -> LIST_ZONED_TIME
            "UUID" -> UUID
            else -> null
        }

        @JvmStatic
        fun toString(type: Neo4jType): String = when (type) {
            ANY -> "ANY"
            LIST_ANY -> "LIST<ANY>"
            BOOLEAN -> "BOOLEAN"
            LIST_BOOLEAN -> "LIST<BOOLEAN>"
            DATE -> "DATE"
            LIST_DATE -> "LIST<DATE>"
            DURATION -> "DURATION"
            LIST_DURATION -> "LIST<DURATION>"
            FLOAT32 -> "FLOAT32"
            LIST_FLOAT32 -> "LIST<FLOAT32>"
            FLOAT -> "FLOAT"
            LIST_FLOAT -> "LIST<FLOAT>"
            INTEGER8 -> "INTEGER8"
            LIST_INTEGER8 -> "LIST<INTEGER8>"
            INTEGER16 -> "INTEGER16"
            LIST_INTEGER16 -> "LIST<INTEGER16>"
            INTEGER32 -> "INTEGER32"
            LIST_INTEGER32 -> "LIST<INTEGER32>"
            INTEGER -> "INTEGER"
            LIST_INTEGER -> "LIST<INTEGER>"
            LOCAL_DATETIME -> "LOCAL DATETIME"
            LIST_LOCAL_DATETIME -> "LIST<LOCAL DATETIME>"
            LOCAL_TIME -> "LOCAL TIME"
            LIST_LOCAL_TIME -> "LIST<LOCAL TIME>"
            POINT -> "POINT"
            LIST_POINT -> "LIST<POINT>"
            STRING -> "STRING"
            LIST_STRING -> "LIST<STRING>"
            VECTOR_FLOAT -> "VECTOR<FLOAT>"
            VECTOR_FLOAT32 -> "VECTOR<FLOAT32>"
            VECTOR_INTEGER -> "VECTOR<INTEGER>"
            VECTOR_INTEGER32 -> "VECTOR<INTEGER32>"
            VECTOR_INTEGER16 -> "VECTOR<INTEGER16>"
            VECTOR_INTEGER8 -> "VECTOR<INTEGER8>"
            ZONED_DATETIME -> "ZONED DATETIME"
            LIST_ZONED_DATETIME -> "LIST<ZONED DATETIME>"
            ZONED_TIME -> "ZONED TIME"
            LIST_ZONED_TIME -> "LIST<ZONED TIME>"
            UUID -> "UUID"
        }

        @JvmStatic
        fun isVector(type: Neo4jType): Boolean = when (type) {
            VECTOR_FLOAT,
            VECTOR_FLOAT32,
            VECTOR_INTEGER,
            VECTOR_INTEGER32,
            VECTOR_INTEGER16,
            VECTOR_INTEGER8 -> true
            else -> false
        }
    }
}
