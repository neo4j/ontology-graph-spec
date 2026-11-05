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
package org.neo4j.spec.graph.model.relationship.extension

import js.objects.Record
import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.emptyRecord
import org.neo4j.spec.graph.model.extension.custom.toCustomClass
import org.neo4j.spec.graph.model.extension.custom.toCustomJs
import org.neo4j.spec.graph.model.index.IndexOptionJs
import org.neo4j.spec.graph.model.index.toClass
import org.neo4j.spec.graph.model.index.toJs
import org.neo4j.spec.graph.model.jso
import org.neo4j.spec.graph.model.relationship.toClass
import org.neo4j.spec.graph.model.relationship.toJs
import org.neo4j.spec.graph.model.type.IndexType

@JsExport
@JsPlainObject
external interface RelationshipIndexJs {
    var type: String
    var properties: Array<String>
    var options: IndexOptionJs?
    val custom: Record<String, Any>
    var name: String?
}

fun relationshipIndexJs(
    type: String,
    properties: Array<String>,
    options: IndexOptionJs? = null,
    custom: Record<String, Any> = emptyRecord(),
    name: String? = null
): RelationshipIndexJs = jso {
    this.type = type
    this.properties = properties
    this.options = options
    this.custom = custom
    this.name = name
}

fun RelationshipIndex.toJs() = relationshipIndexJs(
    type = type.name,
    properties = properties.toTypedArray(),
    options = options?.toJs(),
    custom = custom.toCustomJs(),
    name = name
)

fun RelationshipIndexJs.toClass() = RelationshipIndex(
    type = IndexType.valueOf(type),
    properties = properties.toMutableList(),
    options = options?.toClass(),
    custom = custom.toCustomClass(),
    name = name
)
