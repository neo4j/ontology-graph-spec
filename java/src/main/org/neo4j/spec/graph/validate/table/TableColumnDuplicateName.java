// Generated from org/neo4j/spec/graph/validate/table/TableColumnDuplicateName.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.table;

import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.validate.Issue;

public final class TableColumnDuplicateName implements TableValidation {
    public static final TableColumnDuplicateName INSTANCE = new TableColumnDuplicateName();

    private TableColumnDuplicateName() {
    }

    @Override
    public void validateTableColumn(GraphModel model, String tableId, Table table, int columnIndex,
            TableColumn column, List<Issue> issues) {
        String name = column.getName();
        if (name.isBlank()) {
            return;
        }
        boolean isDuplicate = false;
        for (int otherIndex = 0; otherIndex < table.getColumns().size(); otherIndex++) {
            var other = table.getColumns().get(otherIndex);
            if (otherIndex != columnIndex && Objects.equals(other.getName(), name)) {
                isDuplicate = true;
                break;
            }
        }
        if (isDuplicate) {
            issues.add(new Issue("duplicate_table_column_name", "Duplicate column name '" + name + "' in table '" + tableId + "'", "extensions.data_source_tables." + tableId + ".columns[" + columnIndex + "].name"));
        }
    }
}
