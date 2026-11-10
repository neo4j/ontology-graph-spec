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
package org.neo4j.spec.graph.validate.mapping

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.mapping.Mapping
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping
import org.neo4j.spec.graph.model.extension.mapping.QueryMapping
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MappingDependenciesTest {

    private fun node(id: String = "", dependsOn: String = "") =
        NodeMapping(node = "n", table = "t", properties = mutableMapOf(), id = id, dependsOn = dependsOn)

    private fun rel(id: String = "", dependsOn: String = "") = RelationshipMapping(
        relationship = "r",
        table = "t",
        fromNode = TargetMapping(),
        toNode = TargetMapping(),
        id = id,
        dependsOn = dependsOn
    )

    private fun query(id: String = "", dependsOn: String = "") =
        QueryMapping(table = "t", query = "RETURN 1", id = id, dependsOn = dependsOn)

    private fun validateLists(vararg lists: Pair<String, List<Mapping>>): List<Issue> {
        val model = GraphModel(version = "1", id = "test")
        lists.forEach { (id, mappings) -> model.extensions.dataSourceMappings[id] = mappings.toMutableList() }
        val issues = mutableListOf<Issue>()
        MappingDependencies.validate(model, issues)
        return issues
    }

    private fun validate(vararg mappings: Mapping): List<Issue> = validateLists("list" to mappings.toList())

    @Test
    fun `no mappings is valid`() {
        assertTrue(validate().isEmpty())
        assertTrue(validateLists().isEmpty())
    }

    @Test
    fun `mappings without ids or dependencies are valid`() {
        assertTrue(validate(node(), rel(), query()).isEmpty())
    }

    @Test
    fun `dependency on another mapping is valid for every mapping type`() {
        assertTrue(validate(node("a"), rel("b", "a"), query("c", "b"), node(dependsOn = "c")).isEmpty())
    }

    @Test
    fun `unknown dependency is invalid`() {
        val issues = validate(node("a"), rel(dependsOn = "missing"))
        assertEquals(listOf("mapping_depends_on_unknown"), issues.map { it.code })
        assertEquals("extensions.data_source_mappings.list[1].depends_on", issues.single().path)
        assertEquals("mapping", issues.single().details["keyType"])
        assertEquals("", issues.single().details["key"])
        assertEquals("missing", issues.single().details["dependsOn"])
    }

    @Test
    fun `dependency on a mapping without an id is unknown`() {
        val issues = validate(node(), query(dependsOn = ""), rel(dependsOn = "x"))
        assertEquals(listOf("mapping_depends_on_unknown"), issues.map { it.code })
    }

    @Test
    fun `dependency on a mapping in another list is invalid`() {
        val issues = validateLists(
            "one" to listOf(node("a")),
            "two" to listOf(node("b", "a"))
        )
        assertEquals(listOf("mapping_depends_on_other_list"), issues.map { it.code })
        assertEquals("mapping", issues.single().details["keyType"])
        assertEquals("b", issues.single().details["key"])
        assertEquals("a", issues.single().details["dependsOn"])
        assertEquals("extensions.data_source_mappings.two[0].depends_on", issues.single().path)
    }

    @Test
    fun `dependency on an id in the same list is valid even if another list reuses the id`() {
        val issues = validateLists(
            "one" to listOf(node("a")),
            "two" to listOf(node("a"), node("b", "a"))
        )
        assertTrue(issues.isEmpty())
    }

    @Test
    fun `dependencies are resolved independently for each list`() {
        val issues = validateLists(
            "one" to listOf(node("a"), node(dependsOn = "a")),
            "two" to listOf(node(dependsOn = "missing"))
        )
        assertEquals(listOf("mapping_depends_on_unknown"), issues.map { it.code })
        assertEquals("extensions.data_source_mappings.two[0].depends_on", issues.single().path)
    }

    @Test
    fun `self dependency is invalid`() {
        val issues = validate(node("a", "a"))
        assertEquals(listOf("mapping_depends_on_self"), issues.map { it.code })
        assertEquals("extensions.data_source_mappings.list[0].depends_on", issues.single().path)
        assertEquals("a", issues.single().details["key"])
    }

    @Test
    fun `duplicate ids in a list are invalid`() {
        val issues = validate(node("a"), query("a"), rel("b"))
        assertEquals(listOf("mapping_id_duplicate"), issues.map { it.code })
        assertEquals("extensions.data_source_mappings.list[1].id", issues.single().path)
    }

    @Test
    fun `same id in different lists is valid`() {
        assertTrue(validateLists("one" to listOf(node("a")), "two" to listOf(node("a"))).isEmpty())
    }

    @Test
    fun `two mapping cycle is reported once`() {
        val issues = validate(node("a", "b"), node("b", "a"))
        assertEquals(listOf("mapping_dependency_cycle"), issues.map { it.code })
        assertEquals("extensions.data_source_mappings.list[0].depends_on", issues.single().path)
        assertEquals("a", issues.single().details["key"])
    }

    @Test
    fun `longer cycle with a tail is reported once`() {
        val issues = validate(node("tail", "a"), node("a", "b"), node("b", "c"), node("c", "a"))
        assertEquals(listOf("mapping_dependency_cycle"), issues.map { it.code })
        assertEquals("extensions.data_source_mappings.list[1].depends_on", issues.single().path)
        assertEquals("a", issues.single().details["key"])
    }

    @Test
    fun `separate cycles are each reported`() {
        val issues = validate(node("a", "b"), node("b", "a"), node("c", "d"), node("d", "c"))
        assertEquals(2, issues.count { it.code == "mapping_dependency_cycle" })
    }

    @Test
    fun `long chain is valid`() {
        assertTrue(validate(node("a"), node("b", "a"), node("c", "b"), node("d", "c")).isEmpty())
    }
}
