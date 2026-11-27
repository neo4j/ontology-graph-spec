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

//! The validators that walk data source tables and their columns.

use crate::model::property::Neo4jType;
use crate::model::source::{Table, TableColumn};
use crate::model::GraphModel;

use super::duplicate::duplicate_entry_issues;
use super::{ptr, Issue, Validation};

/// The parts of a table a validator can check, mirroring the reference implementation's
/// table-validation interface: validators override what they need, and the shared walk visits
/// every table and its columns in source order.
pub(crate) trait TableValidation: Validation {
    fn validate_table(
        &self,
        _model: &GraphModel,
        _table_id: &str,
        _table: &Table,
        _issues: &mut Vec<Issue>,
    ) {
    }

    fn validate_table_column(
        &self,
        _model: &GraphModel,
        _table_id: &str,
        _table: &Table,
        _column_index: usize,
        _column: &TableColumn,
        _issues: &mut Vec<Issue>,
    ) {
    }
}

/// Implements the [Validation] walk for a [TableValidation].
macro_rules! table_walk {
    ($validator:ty) => {
        table_walk!($validator, &[]);
    };
    ($validator:ty, $depends:expr) => {
        impl Validation for $validator {
            fn name(&self) -> &'static str {
                stringify!($validator)
            }

            fn depends_on(&self) -> &'static [&'static str] {
                $depends
            }

            fn validate(&self, model: &GraphModel, issues: &mut Vec<Issue>) {
                for (table_id, table) in &model.extensions.data_source_tables {
                    self.validate_table(model, table_id, table, issues);
                    for (column_index, column) in table.columns.iter().enumerate() {
                        self.validate_table_column(
                            model,
                            table_id,
                            table,
                            column_index,
                            column,
                            issues,
                        );
                    }
                }
            }
        }
    };
}

pub(crate) struct TableColumnEmptyName;

impl TableValidation for TableColumnEmptyName {
    fn validate_table_column(
        &self,
        _model: &GraphModel,
        table_id: &str,
        _table: &Table,
        column_index: usize,
        column: &TableColumn,
        issues: &mut Vec<Issue>,
    ) {
        if column.name.trim().is_empty() {
            issues.push(Issue::new(
                "missing_table_column_name",
                format!(
                    "Missing name for table column at index {column_index} in table '{table_id}'"
                ),
                ptr![
                    "extensions",
                    "data_source_tables",
                    table_id,
                    "columns",
                    column_index,
                    "name"
                ],
            ));
        }
    }
}

table_walk!(TableColumnEmptyName);

pub(crate) struct TableColumnDuplicateName;

impl TableValidation for TableColumnDuplicateName {
    fn validate_table_column(
        &self,
        _model: &GraphModel,
        table_id: &str,
        table: &Table,
        column_index: usize,
        column: &TableColumn,
        issues: &mut Vec<Issue>,
    ) {
        let name = column.name.as_str();
        if name.trim().is_empty() {
            return;
        }
        // Flags every column carrying a duplicated name.
        let is_duplicate = table
            .columns
            .iter()
            .enumerate()
            .any(|(other_index, other)| other_index != column_index && other.name == name);
        if is_duplicate {
            issues.push(Issue::new(
                "duplicate_table_column_name",
                format!("Duplicate column name '{name}' in table '{table_id}'"),
                ptr![
                    "extensions",
                    "data_source_tables",
                    table_id,
                    "columns",
                    column_index,
                    "name"
                ],
            ));
        }
    }
}

table_walk!(TableColumnDuplicateName);

pub(crate) struct TableColumnType;

impl TableValidation for TableColumnType {
    fn validate_table_column(
        &self,
        _model: &GraphModel,
        table_id: &str,
        table: &Table,
        column_index: usize,
        column: &TableColumn,
        issues: &mut Vec<Issue>,
    ) {
        // Only tables outside the local source are checked.
        if table.source == "local" {
            return;
        }
        if column.supported.is_empty() {
            issues.push(Issue::new(
                "missing_table_column_supported_type",
                format!("Missing supported types for table column '{}'", column.name),
                ptr![
                    "extensions",
                    "data_source_tables",
                    table_id,
                    "columns",
                    column_index,
                    "supported"
                ],
            ));
        }
    }
}

table_walk!(TableColumnType);

pub(crate) struct TableColumnSupportedDuplicate;

impl TableValidation for TableColumnSupportedDuplicate {
    fn validate_table_column(
        &self,
        _model: &GraphModel,
        table_id: &str,
        _table: &Table,
        column_index: usize,
        column: &TableColumn,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_table_column_supported_type",
            "supported type",
            &format!("table column '{}'", column.name),
            &column
                .supported
                .iter()
                .map(Neo4jType::as_str)
                .map(str::to_owned)
                .collect::<Vec<_>>(),
            &ptr![
                "extensions",
                "data_source_tables",
                table_id,
                "columns",
                column_index,
                "supported"
            ],
        ));
    }
}

table_walk!(TableColumnSupportedDuplicate);
