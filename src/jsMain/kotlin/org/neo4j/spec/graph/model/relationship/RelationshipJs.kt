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

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import org.neo4j.spec.graph.model.associateBy
import org.neo4j.spec.graph.model.emptyRecord
import org.neo4j.spec.graph.model.jso
import org.neo4j.spec.graph.model.property.PropertyJs
import org.neo4j.spec.graph.model.property.extension.toClass
import org.neo4j.spec.graph.model.property.extension.toJs
import org.neo4j.spec.graph.model.property.toClass
import org.neo4j.spec.graph.model.property.toJs
import org.neo4j.spec.graph.model.relationship.extension.RelationshipExtensionsJs
import org.neo4j.spec.graph.model.relationship.extension.relationshipExtensionsJs
import org.neo4j.spec.graph.model.relationship.extension.toClass
import org.neo4j.spec.graph.model.relationship.extension.toJs

@JsExport
@JsPlainObject
external interface RelationshipJs {
    var type: String
    val from: RelationshipTargetJs
    val to: RelationshipTargetJs
    val properties: Record<String, PropertyJs>
    val constraints: Record<String, RelationshipConstraintJs>
    val extensions: RelationshipExtensionsJs
    val id: String
    val description: String
    var aliases: Array<String>
    var reference: String
}

fun relationshipJs(
    type: String,
    from: RelationshipTargetJs = relationshipTargetJs(),
    to: RelationshipTargetJs = relationshipTargetJs(),
    properties: Record<String, PropertyJs> = emptyRecord(),
    constraints: Record<String, RelationshipConstraintJs> = emptyRecord(),
    extensions: RelationshipExtensionsJs = relationshipExtensionsJs(),
    id: String,
    description: String = "",
    aliases: Array<String> = emptyArray(),
    reference: String = ""
): RelationshipJs = jso {
    this.type = type
    this.from = from
    this.to = to
    this.properties = properties
    this.constraints = constraints
    this.extensions = extensions
    this.id = id
    this.description = description
    this.aliases = aliases
    this.reference = reference
}

fun Relationship.toJs(id: String) = relationshipJs(
    type = type,
    from = from.toJs(),
    to = to.toJs(),
    properties = properties.mapValues { (key, property) -> property.toJs(key) }.toRecord(),
    constraints = constraints.mapValues { (_, constraint) -> constraint.toJs() }.toRecord(),
    extensions = extensions.toJs(),
    id = id,
    description = description,
    aliases = aliases.toTypedArray(),
    reference = reference
)

fun RelationshipJs.toClass(id: String) = Relationship(
    type = type,
    from = from.toClass(),
    to = to.toClass(),
    properties = properties.associateBy { key, property -> property.toClass("relationships.$id", key) },
    constraints = constraints.associateBy { _, constraint -> constraint.toClass() },
    extensions = extensions.toClass(),
    description = description,
    aliases = aliases.toMutableList(),
    reference = reference
)
