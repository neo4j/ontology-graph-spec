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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

@JsExport
@Serializable
@SerialName(MappingType.NODE)
@Description("Maps rows of a table to nodes.")
data class NodeMapping @JvmOverloads constructor(
    @Description("Id of the node being mapped.")
    var node: String,
    @Description("Id of the source table.")
    var table: String,
    @Description("Mappings from property ids to source columns.")
    val properties: MutableMap<String, PropertyMapping>,
    @Description("Whether nodes are merged with existing nodes or always created.")
    var mode: MappingMode = MappingMode.MERGE,
    @SerialName("match_label")
    @Description("Label used to match existing nodes.")
    var matchLabel: String? = null,
    @Description("Ids of the properties which identify a node.")
    val key: MutableList<String> = mutableListOf(),
    @SerialName("depends_on")
    @Description(
        "Id of another mapping in the same list which must be imported before this one. " +
            "Empty when there is no dependency."
    )
    var dependsOn: String = "",
    @Description(
        "Optional id of this mapping, so other mappings in the same list can reference it in depends_on."
    )
    var id: String = ""
) : Mapping
