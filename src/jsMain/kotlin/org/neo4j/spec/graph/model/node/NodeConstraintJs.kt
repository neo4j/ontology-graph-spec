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

import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.jso
import org.neo4j.spec.graph.model.node.extension.NodeConstraintExtensionsJs
import org.neo4j.spec.graph.model.node.extension.nodeConstraintExtensionsJs
import org.neo4j.spec.graph.model.node.extension.toClass
import org.neo4j.spec.graph.model.node.extension.toJs
import org.neo4j.spec.graph.model.type.ConstraintType
@JsExport
@JsPlainObject
external interface NodeConstraintJs {
    var type: String
    var properties: Array<String>
    val extensions: NodeConstraintExtensionsJs
    var name: String?
}

fun nodeConstraintJs(
    type: String,
    properties: Array<String> = emptyArray(),
    extensions: NodeConstraintExtensionsJs = nodeConstraintExtensionsJs(),
    name: String? = null
): NodeConstraintJs = jso {
    this.type = type
    this.properties = properties
    this.extensions = extensions
    this.name = name
}

fun NodeConstraint.toJs() = nodeConstraintJs(
    type = type.name,
    properties = properties.toTypedArray(),
    extensions = extensions.toJs(),
    name = name
)

fun NodeConstraintJs.toClass() = NodeConstraint(
    type = ConstraintType.valueOf(type),
    properties = properties.toMutableList(),
    extensions = extensions.toClass(),
    name = name
)
