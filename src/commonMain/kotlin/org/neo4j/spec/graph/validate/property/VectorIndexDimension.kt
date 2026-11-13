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
@file:JvmName("VectorIndexDimensions")

package org.neo4j.spec.graph.validate.property

import org.neo4j.spec.graph.model.index.IndexOption
import org.neo4j.spec.graph.model.index.VectorIndexOption
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.validate.Issue
import kotlin.jvm.JvmName

private const val SYNC_DIMENSION = "Update the property dimension to sync the index dimension."

fun vectorIndexDimensionIssue(
    indexId: String,
    indexType: IndexType,
    indexProperties: List<String>,
    options: IndexOption?,
    properties: Map<String, Property>,
    indexPath: String
): Issue? {
    if (indexType != IndexType.VECTOR || indexProperties.size != 1) return null
    val property = properties[indexProperties.first()] ?: return null
    if (!Neo4jType.isVector(property.type)) return null
    val propertyDimension = property.dimension ?: return null
    val dimensions = (options as? VectorIndexOption)?.dimensions
    return when {
        dimensions == null -> Issue(
            code = "missing_vector_index_dimension",
            message = "Property dimension is missing from the index. $SYNC_DIMENSION",
            path = indexPath
        )
        dimensions != propertyDimension -> Issue(
            code = "mismatched_vector_dimension",
            message = "Index dimension ($dimensions) does not match property dimension ($propertyDimension). " +
                SYNC_DIMENSION,
            path = indexPath
        )
        else -> null
    }
}
