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
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping

// UPX gates mapping validations on isStructuredDataSourceSchema (json-helpers.ts#L333-L337), the data
// source type. graph-spec has no data source type: a model counts as structured when it has data source
// tables or node/relationship mappings. Query-only models count as unstructured.
internal fun GraphModel.hasStructuredDataSource(): Boolean = extensions.dataSourceTables.isNotEmpty() ||
    extensions.dataSourceMappings.values.any { mappings ->
        mappings.any { it is NodeMapping || it is RelationshipMapping }
    }
