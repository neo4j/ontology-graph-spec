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
package org.neo4j.spec.graph.model.extension.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.model.extension.CustomExtensions
import org.neo4j.spec.graph.model.extension.custom.CustomMapSerializer
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

@JsExport
@Serializable
@SerialName("ForeignKeyReference")
@Description("The target of a foreign key.")
data class ForeignKeyReference @JvmOverloads constructor(
    @Description("Id of the referenced table.")
    var table: String,
    @Description("Names of the referenced columns.")
    val columns: MutableList<String> = mutableListOf(),
    @Description("User-defined arbitrary data.")
    @Serializable(with = CustomMapSerializer::class)
    override val custom: MutableMap<String, Any> = mutableMapOf()
) : CustomExtensions
