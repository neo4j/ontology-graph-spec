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
package org.neo4j.spec.graph.validate.relationship

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.extension.mapping.PropertyMapping
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipPropertyMappingMissingTest {

    private val validator = RelationshipPropertyMappingMissing

    private fun relationship() = Relationship(
        type = "KNOWS",
        from = RelationshipTarget(node = "node0"),
        to = RelationshipTarget(node = "node0"),
        properties = mutableMapOf("since" to Property(), "weight" to Property())
    )

    private fun modelWith(mappings: List<RelationshipMapping>) = GraphModel(
        version = "1",
        id = "test",
        extensions = GraphModelExtensions(
            dataSourceTables = mutableMapOf("t" to Table(source = "local")),
            dataSourceMappings = mutableMapOf("mapping0" to mappings.toMutableList())
        )
    )

    private fun mapping(properties: MutableMap<String, PropertyMapping>) = RelationshipMapping(
        relationship = "rel0",
        table = "t",
        fromNode = TargetMapping(),
        toNode = TargetMapping(),
        properties = properties
    )

    private fun validate(model: GraphModel): MutableList<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateRelationship(model, "rel0", relationship(), issues)
        return issues
    }

    @Test
    fun `pass when model has no data source`() {
        val model = GraphModel(version = "1", id = "test")

        assertTrue(validate(model).isEmpty(), "Expected no issues for unstructured models")
    }

    @Test
    fun `pass when the mapping covers every property`() {
        val model = modelWith(
            listOf(
                mapping(mutableMapOf("since" to PropertyMapping("since"), "weight" to PropertyMapping("weight")))
            )
        )

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `fail when a property is covered by no mapping`() {
        val model = modelWith(listOf(mapping(mutableMapOf("since" to PropertyMapping("since")))))

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("missing_property_mapping", issues.first().code)
        assertEquals("relationships.rel0.properties.weight", issues.first().path)
    }

    @Test
    fun `fail for every property when the relationship has no mapping`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf("t" to Table(source = "local"))
            )
        )

        assertEquals(2, validate(model).size)
    }
}
