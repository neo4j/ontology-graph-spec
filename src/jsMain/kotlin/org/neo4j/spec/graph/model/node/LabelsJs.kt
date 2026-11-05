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
import org.neo4j.spec.graph.model.node.extension.LabelsExtensionsJs
import org.neo4j.spec.graph.model.node.extension.labelsExtensionsJs
import org.neo4j.spec.graph.model.node.extension.toClass
import org.neo4j.spec.graph.model.node.extension.toJs

@JsExport
@JsPlainObject
external interface LabelsJs {
    var identifier: String?
    var implied: Array<String>
    var optional: Array<String>
    val extensions: LabelsExtensionsJs
}

fun labelsJs(
    identifier: String? = null,
    implied: Array<String> = emptyArray(),
    optional: Array<String> = emptyArray(),
    extensions: LabelsExtensionsJs = labelsExtensionsJs()
): LabelsJs = jso {
    this.identifier = identifier
    this.implied = implied
    this.optional = optional
    this.extensions = extensions
}

fun Labels.toJs() = labelsJs(
    identifier = identifier,
    implied = implied.toTypedArray(),
    optional = optional.toTypedArray(),
    extensions = extensions.toJs()
)

fun LabelsJs.toClass(): Labels = Labels(
    identifier = identifier,
    implied = implied.toMutableList(),
    optional = optional.toMutableList(),
    extensions = extensions.toClass()
)
