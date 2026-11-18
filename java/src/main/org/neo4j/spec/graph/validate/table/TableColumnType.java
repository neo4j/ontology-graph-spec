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
package org.neo4j.spec.graph.validate.table;

import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.validate.Issue;

public final class TableColumnType implements TableValidation {
    public static final TableColumnType INSTANCE = new TableColumnType();

    private TableColumnType() {}

    @Override
    public void validateTableColumn(
            GraphModel model, String tableId, Table table, int columnIndex, TableColumn column, List<Issue> issues) {
        if (Objects.equals(table.getSource(), "local")) {
            return;
        }
        if (column.getSupported().isEmpty()) {
            issues.add(new Issue(
                    "missing_table_column_type",
                    "Missing suggested type for table column '" + column.getName() + "'",
                    "extensions.data_source_tables." + tableId + ".columns[" + columnIndex + "].type"));
        }
    }
}
