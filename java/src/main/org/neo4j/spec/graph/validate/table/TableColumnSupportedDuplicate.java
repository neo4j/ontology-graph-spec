// Generated from org/neo4j/spec/graph/validate/table/TableColumnSupportedDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.table;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntriesKt;

/**
 * A table column must list each supported type once.
 */
public final class TableColumnSupportedDuplicate implements TableValidation {
    public static final TableColumnSupportedDuplicate INSTANCE = new TableColumnSupportedDuplicate();

    private TableColumnSupportedDuplicate() {
    }

    @Override
    public void validateTableColumn(GraphModel model, String tableId, Table table, int columnIndex,
            TableColumn column, List<Issue> issues) {
        issues.addAll(DuplicateEntriesKt.duplicateEntryIssues("duplicate_table_column_supported_type", "supported type", "table column '" + column.getName() + "'", column.getSupported(), "extensions.data_source_tables." + tableId + ".columns[" + columnIndex + "].supported"));
    }
}
