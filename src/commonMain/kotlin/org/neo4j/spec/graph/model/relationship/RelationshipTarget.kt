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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

@JsExport
@Serializable
@SerialName("RelationshipTarget")
@Description("One end of a relationship.")
data class RelationshipTarget @JvmOverloads constructor(
    @Description("Id of the node at this end.")
    var node: String = "",
    @Description("Label of the node at this end.")
    var label: String = "",
    @Description("Exact number of relationships per node at this end. Null when unconstrained.")
    var count: Int? = null,
    @SerialName("min_count")
    @Description("Minimum number of relationships per node at this end.")
    var minCount: Int? = null,
    @SerialName("max_count")
    @Description("Maximum number of relationships per node at this end.")
    var maxCount: Int? = null
)
