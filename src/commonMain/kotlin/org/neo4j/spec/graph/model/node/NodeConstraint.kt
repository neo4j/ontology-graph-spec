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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.model.node.extension.NodeConstraintExtensions
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.Named
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

@JsExport
@Serializable
@SerialName("NodeConstraint")
@Description("A constraint on a node.")
data class NodeConstraint @JvmOverloads constructor(
    @Description("Type of the constraint.")
    override var type: ConstraintType,
    @Description("Ids of the properties the constraint covers.")
    override val properties: MutableSet<String>,
    @Description("Extensions for the constraint.")
    val extensions: NodeConstraintExtensions = NodeConstraintExtensions(),
    @Description("Name of the constraint in the database.")
    override var name: String? = null
) : Constraint,
    Named
