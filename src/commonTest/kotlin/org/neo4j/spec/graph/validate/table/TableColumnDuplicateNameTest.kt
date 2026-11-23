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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableColumnDuplicateNameTest {

    private val validator = TableColumnDuplicateName
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `fail on duplicate column name - all occurrences flagged`() {
        val table = Table(
            source = "local",
            columns = mutableListOf(
                TableColumn(name = "name"),
                TableColumn(name = "email"),
                TableColumn(name = "name")
            )
        )
        val issuesFirst = mutableListOf<Issue>()
        val issuesThird = mutableListOf<Issue>()

        validator.validateTableColumn(model, "t:1", table, 0, table.columns[0], issuesFirst)
        validator.validateTableColumn(model, "t:1", table, 2, table.columns[2], issuesThird)

        assertEquals(1, issuesFirst.size, "First occurrence should also be flagged as duplicate")
        assertEquals("duplicate_table_column_name", issuesFirst.first().code)
        assertEquals(1, issuesThird.size)
        assertEquals("duplicate_table_column_name", issuesThird.first().code)
    }

    @Test
    fun `pass when all column names are unique`() {
        val table = Table(
            source = "local",
            columns = mutableListOf(
                TableColumn(name = "name"),
                TableColumn(name = "email")
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateTableColumn(model, "t:1", table, 0, table.columns[0], issues)
        validator.validateTableColumn(model, "t:1", table, 1, table.columns[1], issues)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `pass when column name is blank`() {
        // blank names are the empty-name validator's concern, not this one
        val table = Table(
            source = "local",
            columns = mutableListOf(TableColumn(name = ""))
        )
        val issues = mutableListOf<Issue>()

        validator.validateTableColumn(model, "t:1", table, 0, table.columns[0], issues)

        assertTrue(issues.isEmpty())
    }
}
