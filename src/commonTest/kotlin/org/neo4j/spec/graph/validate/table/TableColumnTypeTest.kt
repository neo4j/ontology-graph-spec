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
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableColumnTypeTest {

    private val validator = TableColumnType
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `fail on cloud column with empty supported types`() {
        // UPX: supportedTypes?.length === 0
        val table = Table(
            source = "sql/postgres",
            columns = mutableListOf(
                TableColumn(name = "email", suggested = Neo4jType.STRING, supported = emptyList())
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateTableColumn(model, "t:1", table, 0, table.columns[0], issues)

        assertEquals(1, issues.size)
        assertEquals("missing_table_column_type", issues.first().code)
    }

    @Test
    fun `pass on cloud column with non-empty supported types`() {
        val table = Table(
            source = "sql/postgres",
            columns = mutableListOf(
                TableColumn(
                    name = "email",
                    suggested = Neo4jType.STRING,
                    supported = listOf(Neo4jType.STRING)
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateTableColumn(model, "t:1", table, 0, table.columns[0], issues)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `pass on local column with empty supported types`() {
        // rule skipped for local tables - only cloud columns are checked
        val table = Table(
            source = "local",
            columns = mutableListOf(TableColumn(name = "email", supported = emptyList()))
        )
        val issues = mutableListOf<Issue>()

        validator.validateTableColumn(model, "t:1", table, 0, table.columns[0], issues)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `fail on cloud column with blank name`() {
        // type check is independent of name check - both can fire
        val table = Table(
            source = "sql/postgres",
            columns = mutableListOf(TableColumn(name = "", supported = emptyList()))
        )
        val issues = mutableListOf<Issue>()

        validator.validateTableColumn(model, "t:1", table, 0, table.columns[0], issues)

        assertEquals(1, issues.size)
        assertEquals("missing_table_column_type", issues.first().code)
    }
}
