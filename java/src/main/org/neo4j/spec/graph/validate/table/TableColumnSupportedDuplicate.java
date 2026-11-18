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
// Generated from org/neo4j/spec/graph/validate/table/TableColumnSupportedDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.table;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A table column must list each supported type once.
 */
public final class TableColumnSupportedDuplicate implements TableValidation {
    public static final TableColumnSupportedDuplicate INSTANCE = new TableColumnSupportedDuplicate();

    private TableColumnSupportedDuplicate() {}

    @Override
    public void validateTableColumn(
            GraphModel model, String tableId, Table table, int columnIndex, TableColumn column, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues(
                "duplicate_table_column_supported_type",
                "supported type",
                "table column '" + column.getName() + "'",
                column.getSupported(),
                "extensions.data_source_tables." + tableId + ".columns[" + columnIndex + "].supported"));
    }
}
