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
package org.neo4j.spec.graph.model.index

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

/**
 * https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/create-indexes/#point-indexes-examples
 */
@JsExport
@Serializable
@SerialName("POINT")
@Description("Configuration of a point index.")
data class PointIndexOption @JvmOverloads constructor(
    @SerialName("spatial.cartesian.min")
    @Description("Lower bounds of the 2D cartesian space.")
    val cartesianMin: List<Double> = listOf(-1000000.0, -1000000.0),
    @SerialName("spatial.cartesian.max")
    @Description("Upper bounds of the 2D cartesian space.")
    val cartesianMax: List<Double> = listOf(1000000.0, 1000000.0),
    @SerialName("spatial.cartesian-3d.min")
    @Description("Lower bounds of the 3D cartesian space.")
    val cartesian3DMin: List<Double> = listOf(-1000000.0, -1000000.0, -1000000.0),
    @SerialName("spatial.cartesian-3d.max")
    @Description("Upper bounds of the 3D cartesian space.")
    val cartesian3DMax: List<Double> = listOf(1000000.0, 1000000.0, 1000000.0),
    @SerialName("spatial.wgs-84.min")
    @Description("Lower bounds of the 2D WGS-84 space.")
    val wgs84Min: List<Double> = listOf(-180.0, -90.0),
    @SerialName("spatial.wgs-84.max")
    @Description("Upper bounds of the 2D WGS-84 space.")
    val wgs84Max: List<Double> = listOf(180.0, 90.0),
    @SerialName("spatial.wgs-84-3d.min")
    @Description("Lower bounds of the 3D WGS-84 space.")
    val wgs843DMin: List<Double> = listOf(-180.0, -90.0, -1000000.0),
    @SerialName("spatial.wgs-84-3d.max")
    @Description("Upper bounds of the 3D WGS-84 space.")
    val wgs843DMax: List<Double> = listOf(180.0, 90.0, 1000000.0)
) : IndexOption {
    override val type: IndexType get() = IndexType.POINT
}
