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
import org.neo4j.spec.graph.model.node.extension.LabelsExtensions
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

@JsExport
@Serializable
@SerialName("Labels")
@Description("The labels of a node.")
data class Labels @JvmOverloads constructor(
    @Description("The label which identifies the node.")
    var identifier: String? = null,
    @Description("Labels implied by the node in addition to the identifier.")
    val implied: MutableList<String> = mutableListOf(),
    @Description("Labels which may be present on the node but aren't required.")
    val optional: MutableList<String> = mutableListOf(),
    @Description("Extensions for the labels.")
    val extensions: LabelsExtensions = LabelsExtensions()
)
