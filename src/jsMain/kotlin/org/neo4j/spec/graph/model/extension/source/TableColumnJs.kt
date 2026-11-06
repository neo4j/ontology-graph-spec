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
package org.neo4j.spec.graph.model.extension.source

import js.objects.Record
import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.emptyRecord
import org.neo4j.spec.graph.model.extension.custom.toCustomClass
import org.neo4j.spec.graph.model.extension.custom.toCustomJs
import org.neo4j.spec.graph.model.jso
import org.neo4j.spec.graph.model.property.Neo4jType
import kotlin.String

@JsExport
@JsPlainObject
external interface TableColumnJs {
    val name: String
    var type: String
    var size: Int
    val suggested: String
    val supported: Array<String>
    val nullable: Boolean
    val dimension: Int?
    val custom: Record<String, Any>
}

fun tableColumnJs(
    name: String,
    type: String,
    size: Int = -1,
    suggested: String = "ANY",
    supported: Array<String> = emptyArray(),
    nullable: Boolean = true,
    dimension: Int? = null,
    custom: Record<String, Any> = emptyRecord()
): TableColumnJs = jso {
    this.name = name
    this.type = type
    this.size = size
    this.suggested = suggested
    this.supported = supported
    this.nullable = nullable
    this.dimension = dimension
    this.custom = custom
}

fun TableColumn.toJs() = tableColumnJs(
    name = name,
    type = type,
    size = size,
    suggested = Neo4jType.toString(suggested),
    supported = supported.map { Neo4jType.toString(it) }.toTypedArray(),
    nullable = nullable,
    dimension = dimension,
    custom = custom.toCustomJs()
)

fun TableColumnJs.toClass() = TableColumn(
    name = name,
    type = type,
    size = size,
    suggested = neo4jType(suggested),
    supported = supported.map { neo4jType(it) },
    nullable = nullable,
    dimension = dimension,
    custom = custom.toCustomClass()
)

private fun TableColumnJs.neo4jType(type: String) = Neo4jType.fromString(type)
    ?: error("Invalid neo4j type '$type' for column '$name'")
