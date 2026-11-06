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
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.extension.source.TableColumn
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableColumnSupportedDuplicateTest {

    private fun validate(supported: List<Neo4jType>): List<Issue> {
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf(
                    "t" to Table(
                        source = "local",
                        columns = mutableListOf(
                            TableColumn(name = "c", suggested = Neo4jType.STRING, supported = supported)
                        )
                    )
                )
            )
        )
        return mutableListOf<Issue>().also { TableColumnSupportedDuplicate.validate(model, it) }
    }

    @Test
    fun `pass when supported types are unique`() {
        assertTrue(validate(listOf(Neo4jType.STRING, Neo4jType.INTEGER)).isEmpty())
    }

    @Test
    fun `fail on repeated supported type`() {
        val issues = validate(listOf(Neo4jType.STRING, Neo4jType.STRING))

        assertEquals(1, issues.size)
        assertEquals("duplicate_table_column_supported_type", issues.first().code)
        assertEquals("extensions.data_source_tables.t.columns[0].supported[1]", issues.first().path)
    }
}
