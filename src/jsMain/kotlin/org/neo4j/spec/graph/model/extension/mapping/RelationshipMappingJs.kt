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
package org.neo4j.spec.graph.model.extension.mapping

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.associateBy
import org.neo4j.spec.graph.model.jso
@JsExport
@JsPlainObject
external interface RelationshipMappingJs : MappingJs {
    override val type: String
    var relationship: String
    var table: String
    val fromNode: TargetMappingJs
    val toNode: TargetMappingJs
    val properties: Record<String, PropertyMappingJs>
    var mode: String
    var matchLabel: String?
    var key: Array<String>
    var dependsOn: String
    var id: String
}

fun relationshipMappingJs(
    relationship: String,
    table: String,
    fromNode: TargetMappingJs,
    toNode: TargetMappingJs,
    properties: Record<String, PropertyMappingJs>,
    mode: String,
    matchLabel: String?,
    key: Array<String>,
    dependsOn: String = "",
    id: String = ""
): RelationshipMappingJs = jso {
    this.type = MappingType.RELATIONSHIP
    this.relationship = relationship
    this.table = table
    this.fromNode = fromNode
    this.toNode = toNode
    this.properties = properties
    this.mode = mode
    this.matchLabel = matchLabel
    this.key = key
    this.dependsOn = dependsOn
    this.id = id
}

fun RelationshipMapping.toJs() = relationshipMappingJs(
    relationship = relationship,
    table = table,
    fromNode = fromNode.toJs(),
    toNode = toNode.toJs(),
    properties = properties.map { it.key to it.value.toJs() }.toMap().toRecord(),
    mode = mode.name,
    matchLabel = matchLabel,
    key = key.toTypedArray(),
    dependsOn = dependsOn,
    id = id
)

fun RelationshipMappingJs.toClass() = RelationshipMapping(
    relationship = relationship,
    table = table,
    fromNode = fromNode.toClass(),
    toNode = toNode.toClass(),
    properties = properties.associateBy { _, value -> value.toClass() },
    mode = MappingMode.valueOf(mode),
    matchLabel = matchLabel,
    key = key.toMutableList(),
    dependsOn = dependsOn,
    id = id
)
