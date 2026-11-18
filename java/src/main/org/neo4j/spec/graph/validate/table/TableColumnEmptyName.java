// Generated from org/neo4j/spec/graph/validate/table/TableColumnEmptyName.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.table;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.validate.Issue;

public final class TableColumnEmptyName implements TableValidation {
    public static final TableColumnEmptyName INSTANCE = new TableColumnEmptyName();

    private TableColumnEmptyName() {
    }

    @Override
    public void validateTableColumn(GraphModel model, String tableId, Table table, int columnIndex,
            TableColumn column, List<Issue> issues) {
        if (Kt.isBlank(column.getName())) {
            issues.add(new Issue("missing_table_column_name", "Missing name for table column at index " + columnIndex + " in table '" + tableId + "'", "extensions.data_source_tables." + tableId + ".columns[" + columnIndex + "].name"));
        }
    }
}
