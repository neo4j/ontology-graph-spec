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
package org.neo4j.spec.graph.validate.table

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.extension.source.TableColumn
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.pathOf
import kotlin.js.JsExport
@JsExport
object TableColumnEmptyName : TableValidation {
    override fun validateTableColumn(
        model: GraphModel,
        tableId: String,
        table: Table,
        columnIndex: Int,
        column: TableColumn,
        issues: MutableList<Issue>
    ) {
        if (column.name.isBlank()) {
            issues.add(
                Issue(
                    code = "missing_table_column_name",
                    message = "Missing name for table column at index $columnIndex in table '$tableId'",
                    path = pathOf("extensions", "data_source_tables", tableId, "columns", columnIndex, "name"),
                    details = issueDetails("column", column.name)
                )
            )
        }
    }
}
