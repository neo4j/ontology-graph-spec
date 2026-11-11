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
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipPropertyUnsupportedLocalImportTypeTest {

    private val validator = RelationshipPropertyUnsupportedLocalImportType

    private fun model(propertyType: Neo4jType, tableSource: String): Pair<GraphModel, Relationship> {
        val relationship = Relationship(
            type = "KNOWS",
            from = RelationshipTarget(node = "node0"),
            to = RelationshipTarget(node = "node0"),
            properties = mutableMapOf("since" to Property(type = propertyType))
        )
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf("t" to Table(source = tableSource)),
                dataSourceMappings = mutableMapOf(
                    "mapping0" to mutableListOf(
                        RelationshipMapping(
                            relationship = "rel0",
                            table = "t",
                            fromNode = TargetMapping(),
                            toNode = TargetMapping()
                        )
                    )
                )
            )
        )
        return model to relationship
    }

    private fun validate(model: GraphModel, relationship: Relationship): MutableList<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateRelationship(model, "rel0", relationship, issues)
        return issues
    }

    @Test
    fun `pass when relationship has no mapping`() {
        val relationship = Relationship(
            type = "KNOWS",
            from = RelationshipTarget(node = "node0"),
            to = RelationshipTarget(node = "node0"),
            properties = mutableMapOf("since" to Property(type = Neo4jType.DATE))
        )
        val model = GraphModel(version = "1", id = "test")
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "rel0", relationship, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when relationship has no mapping")
    }

    @Test
    fun `pass when mapping targets a non-local table`() {
        val (model, relationship) = model(Neo4jType.DATE, "cloud")

        assertTrue(validate(model, relationship).isEmpty(), "Expected no issues for non-local tables")
    }

    @Test
    fun `pass when local table and property type is supported`() {
        val (model, relationship) = model(Neo4jType.INTEGER, "local")

        assertTrue(validate(model, relationship).isEmpty())
    }

    @Test
    fun `pass when local table and property type is unset`() {
        val (model, relationship) = model(Neo4jType.ANY, "local")

        assertTrue(validate(model, relationship).isEmpty(), "Expected no issues when the type is unset")
    }

    @Test
    fun `fail when local table and property type is unsupported`() {
        val (model, relationship) = model(Neo4jType.DURATION, "local")

        val issues = validate(model, relationship)

        assertEquals(1, issues.size)
        assertEquals("unsupported_local_import_property_type", issues.first().code)
        assertEquals("relationships.rel0.properties.since.type", issues.first().path)
    }
}
