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
package org.neo4j.spec.graph.model.node

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.associateBy
import org.neo4j.spec.graph.model.emptyRecord
import org.neo4j.spec.graph.model.jso
import org.neo4j.spec.graph.model.node.extension.NodeExtensionsJs
import org.neo4j.spec.graph.model.node.extension.nodeExtensionsJs
import org.neo4j.spec.graph.model.node.extension.toClass
import org.neo4j.spec.graph.model.node.extension.toJs
import org.neo4j.spec.graph.model.property.PropertyJs
import org.neo4j.spec.graph.model.property.extension.toClass
import org.neo4j.spec.graph.model.property.extension.toJs
import org.neo4j.spec.graph.model.property.toClass
import org.neo4j.spec.graph.model.property.toJs
import kotlin.collections.component1
import kotlin.collections.component2

@JsExport
@JsPlainObject
external interface NodeJs {
    val labels: LabelsJs
    val properties: Record<String, PropertyJs>
    val constraints: Record<String, NodeConstraintJs>
    val extensions: NodeExtensionsJs
    val id: String
    val description: String
    var aliases: Array<String>
    var reference: String
}

fun nodeJs(
    labels: LabelsJs = labelsJs(),
    properties: Record<String, PropertyJs> = emptyRecord(),
    constraints: Record<String, NodeConstraintJs> = emptyRecord(),
    extensions: NodeExtensionsJs = nodeExtensionsJs(),
    id: String,
    description: String = "",
    aliases: Array<String> = emptyArray(),
    reference: String = ""
): NodeJs = jso {
    this.labels = labels
    this.properties = properties
    this.constraints = constraints
    this.extensions = extensions
    this.id = id
    this.description = description
    this.aliases = aliases
    this.reference = reference
}

fun Node.toJs(key: String) = nodeJs(
    labels = labels.toJs(),
    properties = properties.mapValues { (key, property) -> property.toJs(key) }.toRecord(),
    constraints = constraints.mapValues { (_, constraint) -> constraint.toJs() }.toRecord(),
    extensions = extensions.toJs(),
    id = key,
    description = description,
    aliases = aliases.toTypedArray(),
    reference = reference
)

fun NodeJs.toClass(id: String): Node = Node(
    labels = labels.toClass(),
    properties = properties.associateBy { key, value -> value.toClass("nodes.$id", key) },
    constraints = constraints.associateBy { _, value -> value.toClass() },
    extensions = extensions.toClass(),
    description = description,
    aliases = aliases.toMutableList(),
    reference = reference
)
