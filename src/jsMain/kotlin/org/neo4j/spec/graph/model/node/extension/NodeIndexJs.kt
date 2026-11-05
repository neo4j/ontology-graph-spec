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
package org.neo4j.spec.graph.model.node.extension

import js.objects.Record
import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.emptyRecord
import org.neo4j.spec.graph.model.extension.custom.toCustomClass
import org.neo4j.spec.graph.model.extension.custom.toCustomJs
import org.neo4j.spec.graph.model.index.IndexOptionJs
import org.neo4j.spec.graph.model.index.toClass
import org.neo4j.spec.graph.model.index.toJs
import org.neo4j.spec.graph.model.jso
import org.neo4j.spec.graph.model.node.toClass
import org.neo4j.spec.graph.model.node.toJs
import org.neo4j.spec.graph.model.type.IndexType

@JsExport
@JsPlainObject
external interface NodeIndexJs {
    var type: String
    var labels: Array<String>
    var properties: Array<String>
    var options: IndexOptionJs?
    val custom: Record<String, Any>
    var name: String?
}

fun nodeIndexJs(
    type: String,
    labels: Array<String> = emptyArray(),
    properties: Array<String> = emptyArray(),
    options: IndexOptionJs? = null,
    custom: Record<String, Any> = emptyRecord(),
    name: String? = null
): NodeIndexJs = jso {
    this.type = type
    this.labels = labels
    this.properties = properties
    this.options = options
    this.custom = custom
    this.name = name
}

fun NodeIndex.toJs() = nodeIndexJs(
    type = type.name,
    labels = labels.toTypedArray(),
    properties = properties.toTypedArray(),
    options = options?.toJs(),
    custom = custom.toCustomJs(),
    name = name
)

fun NodeIndexJs.toClass() = NodeIndex(
    type = IndexType.valueOf(type),
    labels = labels.toMutableList(),
    properties = properties.toMutableList(),
    options = options?.toClass(),
    custom = custom.toCustomClass(),
    name = name
)
