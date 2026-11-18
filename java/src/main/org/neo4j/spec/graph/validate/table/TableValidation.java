// Generated from org/neo4j/spec/graph/validate/table/TableValidation.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.table;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;

public interface TableValidation extends Validation {
    @Override
    default void validate(GraphModel model, List<Issue> issues) {
        for (var entry : model.getExtensions().getDataSourceTables().entrySet()) {
            var tableId = entry.getKey();
            var table = entry.getValue();
            validateTable(model, tableId, table, issues);
            for (var item : Kt.withIndex(table.getColumns())) {
                var index = item.getIndex();
                var column = item.getValue();
                validateTableColumn(model, tableId, table, index, column, issues);
            }
        }
    }

    default void validateTable(GraphModel model, String tableId, Table table, List<Issue> issues) {
    }

    default void validateTableColumn(GraphModel model, String tableId, Table table, int columnIndex,
            TableColumn column, List<Issue> issues) {
    }
}
