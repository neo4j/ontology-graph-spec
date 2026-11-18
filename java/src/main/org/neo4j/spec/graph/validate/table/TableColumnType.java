// Generated from org/neo4j/spec/graph/validate/table/TableColumnType.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.table;

import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.validate.Issue;

public final class TableColumnType implements TableValidation {
    public static final TableColumnType INSTANCE = new TableColumnType();

    private TableColumnType() {
    }

    @Override
    public void validateTableColumn(GraphModel model, String tableId, Table table, int columnIndex,
            TableColumn column, List<Issue> issues) {
        if (Objects.equals(table.getSource(), "local")) {
            return;
        }
        if (column.getSupported().isEmpty()) {
            issues.add(new Issue("missing_table_column_type", "Missing suggested type for table column '" + column.getName() + "'", "extensions.data_source_tables." + tableId + ".columns[" + columnIndex + "].type"));
        }
    }
}
