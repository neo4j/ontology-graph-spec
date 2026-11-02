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
package org.neo4j.spec.graph.model.extension

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.custom.CustomMapSerializer
import org.neo4j.spec.graph.model.extension.mapping.Mapping
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

/**
 * Extensions for a [GraphModel].
 */
@JsExport
@Serializable
@SerialName("GraphModelExtensions")
@Description("Extensions for a graph model.")
data class GraphModelExtensions @JvmOverloads constructor(
    @Description("Data source table schemas keyed by table id.")
    @SerialName("data_source_tables")
    val dataSourceTables: MutableMap<String, Table> = mutableMapOf(),
    @Description("Sets of data source mappings from tables to nodes and relationships, often used for importing.")
    @SerialName("data_source_mappings")
    val dataSourceMappings: MutableMap<String, MutableList<Mapping>> = mutableMapOf(),
    @Description("User-defined arbitrary data.")
    @Serializable(with = CustomMapSerializer::class)
    override val custom: MutableMap<String, Any> = mutableMapOf()
) : CustomExtensions
