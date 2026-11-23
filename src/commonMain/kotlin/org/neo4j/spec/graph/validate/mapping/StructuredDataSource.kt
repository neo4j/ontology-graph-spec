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
package org.neo4j.spec.graph.validate.mapping

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping
import org.neo4j.spec.graph.model.extension.mapping.QueryMapping
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping

internal fun GraphModel.hasMappings(): Boolean =
    extensions.dataSourceMappings.values.flatten().any { it is NodeMapping || it is RelationshipMapping }

internal fun GraphModel.hasQueryMappings(): Boolean =
    extensions.dataSourceMappings.values.flatten().any { it is QueryMapping }

internal fun GraphModel.hasDataSourceTables(): Boolean = extensions.dataSourceTables.isNotEmpty()

internal fun GraphModel.hasStructuredDataSource(): Boolean = when {
    hasMappings() -> true
    hasQueryMappings() -> false
    hasDataSourceTables() -> true
    else -> false
}
