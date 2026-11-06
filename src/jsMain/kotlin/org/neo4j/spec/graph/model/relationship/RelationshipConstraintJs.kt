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
package org.neo4j.spec.graph.model.relationship

import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.index.toClass
import org.neo4j.spec.graph.model.jso
import org.neo4j.spec.graph.model.relationship.extension.RelationshipConstraintExtensionsJs
import org.neo4j.spec.graph.model.relationship.extension.relationshipConstraintExtensionsJs
import org.neo4j.spec.graph.model.relationship.extension.toClass
import org.neo4j.spec.graph.model.relationship.extension.toJs
import org.neo4j.spec.graph.model.type.ConstraintType
@JsExport
@JsPlainObject
external interface RelationshipConstraintJs {
    var type: String
    var properties: Array<String>
    val extensions: RelationshipConstraintExtensionsJs
    var name: String?
}

fun relationshipConstraintJs(
    type: String,
    properties: Array<String> = emptyArray(),
    extensions: RelationshipConstraintExtensionsJs = relationshipConstraintExtensionsJs(),
    name: String? = null
): RelationshipConstraintJs = jso {
    this.type = type
    this.properties = properties
    this.extensions = extensions
    this.name = name
}

fun RelationshipConstraint.toJs() = relationshipConstraintJs(
    type = type.name,
    properties = properties.toTypedArray(),
    extensions = extensions.toJs(),
    name = name
)

fun RelationshipConstraintJs.toClass() = RelationshipConstraint(
    type = ConstraintType.valueOf(type),
    properties = properties.toMutableList(),
    extensions = extensions.toClass(),
    name = name
)
