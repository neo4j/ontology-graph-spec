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
package org.neo4j.spec.graph.model.property

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.model.property.extension.PropertyExtensions
import org.neo4j.spec.graph.model.type.Named
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport

@JsExport
@Serializable
@SerialName("Property")
@Description("A property of a node or relationship.")
data class Property(
    @Description("Neo4j type of the property.")
    var type: Neo4jType = Neo4jType.ANY,
    @Description("Dimension of the property when it holds a vector.")
    var dimension: Int? = null,
    @SerialName("must_exist")
    @Description(
        "Shorthand for an existence constraint. Expanded into constraints when the model is decoded."
    )
    var mustExist: Boolean? = null,
    @Description(
        "Shorthand for a uniqueness constraint. Expanded into constraints when the model is decoded."
    )
    var unique: Boolean? = null,
    @Description("Shorthand for a key constraint. Expanded into constraints when the model is decoded.")
    var key: Boolean? = null,
    @Description("Extensions for the property.")
    val extensions: PropertyExtensions = PropertyExtensions(),
    @Description("Name of the property in the database, when different from its id.")
    override var name: String? = null,
    @Description("Description of the property.")
    val description: String = "",
    @Description("Alternative names for the property.")
    val aliases: MutableSet<String> = mutableSetOf(),
    @Description("Reference or URI of an external resource describing the property.")
    val reference: String = "",
    @Description("Pattern values of the property are expected to match.")
    val pattern: String = "",
    @SerialName("one_of")
    @Description("Values the property is restricted to.")
    val oneOf: MutableList<String> = mutableListOf()
) : Named
