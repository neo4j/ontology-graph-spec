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
package org.neo4j.spec.graph.model

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.schema.Description
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.Validation
import org.neo4j.spec.graph.validate.ValidationTree
import kotlin.js.JsExport
import kotlin.js.JsStatic

@JsExport
@Serializable
@SerialName("GraphModel")
data class GraphModel(
    @Description("Schema specification uri used.")
    @Required
    val schema: String = Version.schemaUrl(Version.LATEST),
    @Description(
        "User-defined version of this model document, e.g. a revision number or label."
    )
    val version: String,
    @Description(
        "Unique identifier of the model, typically a kebab-case version of the name, used for referencing."
    )
    val id: String,
    @Description("Human readable name for the model.")
    val name: String = "",
    @Description("Description of the model.")
    val description: String = "",
    @Description("Node definitions keyed by node id.")
    val nodes: MutableMap<String, Node> = mutableMapOf(),
    @Description("Relationship definitions keyed by relationship id.")
    val relationships: MutableMap<String, Relationship> = mutableMapOf(),
    @Description("Extensions to the model: data source tables, mappings and external custom data.")
    val extensions: GraphModelExtensions = GraphModelExtensions()
) {
    /**
     * Expands any shorthand in the model into its longhand form, see [Notation].
     */
    @JsExport.Ignore
    fun longhand() {
        Notation.longhand(this)
    }

    /**
     * Collapses longhand in the model into shorthand wherever that loses no information, see [Notation].
     */
    @JsExport.Ignore
    fun shorthand() {
        Notation.shorthand(this)
    }

    @JsExport.Ignore
    fun validate(validators: List<Validation>): List<Issue> {
        val tree = ValidationTree()
        tree.build(validators)
        return tree.validate(this)
    }

    companion object {
        @JsStatic
        fun validate(model: GraphModel, validators: List<Validation>) = model.validate(validators)

        @JsStatic
        fun longhand(model: GraphModel) = model.longhand()

        @JsStatic
        fun shorthand(model: GraphModel) = model.shorthand()
    }
}
