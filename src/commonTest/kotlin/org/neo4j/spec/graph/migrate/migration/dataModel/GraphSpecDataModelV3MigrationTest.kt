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
package org.neo4j.spec.graph.migrate.migration.dataModel

import org.neo4j.spec.graph.codec.schema.SchemaLiteral
import org.neo4j.spec.graph.codec.schema.SchemaNull
import org.neo4j.spec.graph.codec.schema.schemaListOf
import org.neo4j.spec.graph.codec.schema.schemaMapOf
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphSpecDataModelV3MigrationTest {

    private val migration = GraphSpecDataModelV3Migration()

    @Test
    fun `migrate converts node labels and properties correctly`() {
        val input = schemaMapOf(
            "version" to "2.0",
            "nodes" to schemaMapOf(
                "node1" to schemaMapOf(
                    "labels" to schemaMapOf(
                        "identifier" to "Person",
                        "implied" to listOf("Entity")
                    ),
                    "properties" to schemaMapOf(
                        "p1" to schemaMapOf(
                            "name" to "name",
                            "type" to "STRING",
                            "mustExist" to true,
                            "description" to "prop description"
                        )
                    ),
                    "description" to "a description"
                )
            )
        )

        val result = migration.migrate(input)

        // Verify Node Labels
        val nodeLabels = result
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMaps("nodeLabels")
        assertEquals(2, nodeLabels.size)
        val personLabel = nodeLabels.first { it.string("token") == "Person" }
        assertEquals("nl:0", personLabel.string("\$id"))

        // Verify Properties moved to Label
        val props = personLabel.listOfMaps("properties")
        assertEquals("p1", props[0].string("\$id"))
        assertEquals("string", props[0].map("type").string("type"))
        assertEquals("prop description", props[0].string("description"))

        // Verify Object Type
        val nodeObjectTypes = result
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMaps("nodeObjectTypes")
        assertEquals("node1", nodeObjectTypes[0].string("\$id"))
        assertEquals("a description", nodeObjectTypes[0].string("description"))
        val labelRefs = nodeObjectTypes[0].listOfMaps("labels")
        assertEquals("#nl:0", labelRefs[0].string("\$ref"))
    }

    @Test
    fun `migrate deduplicates properties when multiple nodes share the same primary label`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING"))
                ),
                "n2" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING"))
                )
            )
        )

        val result = migration.migrate(input)
        val nodeLabels = result
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMaps("nodeLabels")

        // The logic creates a new ID nl:0, nl:1 for every label found in the loop.
        // However, the test verifies if the properties list for a specific label ID remains unique.
        val personLabel = nodeLabels.first { it.string("token") == "Person" }
        val props = personLabel.listOfMaps("properties")
        assertEquals(1, props.size, "Property p1 should not be duplicated in the label definition")
    }

    @Test
    fun `migrate converts relationships and handles ObjectType references`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf("n1" to schemaMapOf("labels" to schemaMapOf("identifier" to "A"))),
            "relationships" to schemaMapOf(
                "rel1" to schemaMapOf(
                    "type" to "WORKS_AT",
                    "from" to schemaMapOf("node" to "n1"),
                    "to" to schemaMapOf("node" to "n2"),
                    "properties" to schemaMapOf(
                        "since" to schemaMapOf(
                            "name" to "since",
                            "type" to "INTEGER",
                            "description" to "prop description"
                        )
                    ),
                    "description" to "a description"
                )
            )
        )

        val result = migration.migrate(input)
        val schema = result.map("graphSchemaRepresentation").map("graphSchema")

        // Verify Relationship Type
        val relTypes = schema.listOfMaps("relationshipTypes")
        assertEquals("rt:0", relTypes[0].string("\$id"))
        assertEquals("prop description", relTypes[0].listOfMaps("properties")[0].string("description"))

        // Verify Relationship Object Type (the link between nodes)
        val relObjectTypes = schema.listOfMaps("relationshipObjectTypes")
        assertEquals("rel1", relObjectTypes[0].string("\$id"))
        assertEquals("a description", relObjectTypes[0].string("description"))
        assertEquals("#rt:0", relObjectTypes[0].map("type").string("\$ref"))
        assertEquals("#n1", relObjectTypes[0].map("from").string("\$ref"))
    }

    @Test
    fun `migrate handles relationship constraints and sets relationshipType ref`() {
        val input = schemaMapOf(
            "relationships" to schemaMapOf(
                "r:1" to schemaMapOf(
                    "type" to "LIVES_IN",
                    "from" to schemaMapOf("node" to "n1"),
                    "to" to schemaMapOf("node" to "n2"),
                    "constraints" to schemaMapOf(
                        "rel_uniq" to schemaMapOf("type" to "UNIQUE", "properties" to listOf("p1"))
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val constraints = result.map("graphSchemaRepresentation")
            .map("graphSchema").listOfMaps("constraints")

        val relConstraint = constraints[0]
        assertEquals("rel_uniq", relConstraint.string("\$id"))
        assertEquals("relationship", relConstraint.string("entityType"))
        assertEquals("#rt:0", relConstraint.map("relationshipType").string("\$ref"))
        // Ensure nodeLabel is omitted rather than null
        assertFalse(relConstraint.containsKey("nodeLabel"))
    }

    @Test
    fun `migrate handles constraints and indexes with type transformation`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "User"),
                    "constraints" to schemaMapOf(
                        "uniq_id" to schemaMapOf("type" to "UNIQUE", "properties" to listOf("p1"))
                    ),
                    "extensions" to schemaMapOf(
                        "indexes" to schemaMapOf(
                            "idx_name" to schemaMapOf("type" to "RANGE", "properties" to listOf("p2"))
                        )
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val graphSchema = result.map("graphSchemaRepresentation").map("graphSchema")

        val constraint = graphSchema.listOfMaps("constraints")[0]
        assertEquals("uniqueness", constraint.string("constraintType"))
        assertEquals("#nl:0", constraint.map("nodeLabel").string("\$ref"))
        assertFalse(constraint.containsKey("relationshipType"))

        val index = graphSchema.listOfMaps("indexes")[0]
        assertEquals("range", index.string("indexType"))
    }

    @Test
    fun `migrate maps each constraint and index type to its data model word`() {
        val constraintTypes = listOf("UNIQUE", "KEY", "EXISTS", "PROPERTY_TYPE")
        val indexTypes = listOf("RANGE", "TEXT", "FULLTEXT", "POINT", "VECTOR", "LOOKUP")
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "User"),
                    "constraints" to constraintTypes.associate { type ->
                        "c_$type" to schemaMapOf("type" to type, "properties" to listOf("p1"))
                    },
                    "extensions" to schemaMapOf(
                        "indexes" to indexTypes.associate { type ->
                            "i_$type" to schemaMapOf("type" to type, "properties" to listOf("p1"))
                        }
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val graphSchema = result.map("graphSchemaRepresentation").map("graphSchema")

        assertEquals(
            mapOf(
                "c_UNIQUE" to "uniqueness",
                "c_KEY" to "key",
                "c_EXISTS" to "propertyExistence",
                "c_PROPERTY_TYPE" to "propertyType"
            ),
            graphSchema.listOfMaps("constraints").associate { it.string("\$id") to it.string("constraintType") }
        )
        assertEquals(
            mapOf(
                "i_RANGE" to "range",
                "i_TEXT" to "text",
                "i_FULLTEXT" to "fullText",
                "i_POINT" to "point",
                "i_VECTOR" to "vector",
                "i_LOOKUP" to "lookup"
            ),
            graphSchema.listOfMaps("indexes").associate { it.string("\$id") to it.string("indexType") }
        )
        assertEquals(
            "p1_User_type",
            graphSchema.listOfMaps("constraints").first { it.string("\$id") == "c_PROPERTY_TYPE" }.string("name")
        )
    }

    @Test
    fun `convertGraphMapping correctly recovers relationship IDs via findRelationshipId`() {
        val input = schemaMapOf(
            "relationships" to schemaMapOf(
                "actual_rel_id" to schemaMapOf(
                    "type" to "FOLLOWS",
                    "from" to schemaMapOf("node" to "User"),
                    "to" to schemaMapOf("node" to "User")
                )
            ),
            "extensions" to schemaMapOf(
                "data_source_mappings" to schemaMapOf(
                    "m0" to listOf(
                        schemaMapOf(
                            "relationship" to "actual_rel_id",
                            "table" to "user_follows",
                            "from_node" to
                                schemaMapOf(
                                    "properties" to schemaMapOf("uid" to schemaMapOf("column" to "from_id"))
                                ),
                            "to_node" to
                                schemaMapOf(
                                    "properties" to schemaMapOf("uid" to schemaMapOf("column" to "to_id"))
                                )
                        )
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val relMappings = result
            .map("graphMappingRepresentation")
            .listOfMaps("relationshipMappings")

        assertEquals(1, relMappings.size)
        // Verify that findRelationshipId matched "FOLLOWS" + "User" -> "User" to "actual_rel_id"
        assertEquals("#actual_rel_id", relMappings[0].map("relationship").string("\$ref"))
        assertEquals("user_follows", relMappings[0].string("tableName"))
    }

    @Test
    fun `convertGraphMapping ignores relationship mappings that cannot be resolved`() {
        val input = schemaMapOf(
            "relationships" to schemaMapOf(), // Empty
            "extensions" to schemaMapOf(
                "data_source_mappings" to schemaMapOf(
                    "m0" to listOf(
                        schemaMapOf(
                            "relationship" to "NON_EXISTENT",
                            "from_node" to schemaMapOf(),
                            "to_node" to schemaMapOf()
                        )
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val relMappings = result
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("relationshipMappings")
        assertEquals(emptyList(), relMappings)
    }

    @Test
    fun `convertExtensions identifies key properties from mappings`() {
        val input = schemaMapOf(
            "extensions" to schemaMapOf(
                "data_source_mappings" to schemaMapOf(
                    "m0" to listOf(
                        schemaMapOf(
                            "node" to "node1",
                            "table" to "table1",
                            "properties" to schemaMapOf(
                                "p1" to schemaMapOf("field" to "field1"),
                                "p2" to schemaMapOf("field" to "field2"),
                                "p3" to schemaMapOf("field" to "field3")
                            ),
                            "key" to schemaListOf("p1")
                        ),
                        schemaMapOf(
                            "relationship" to "relationship1",
                            "table" to "table2",
                            "properties" to schemaMapOf(
                                "p1" to schemaMapOf("field" to "field1"),
                                "p2" to schemaMapOf("field" to "field2"),
                                "p3" to schemaMapOf("field" to "field3")
                            ),
                            "key" to schemaListOf("p2", "p3")
                        )
                    )
                )
            )
        )

        val result = migration.convertExtensions(input)
        assertNotNull(result)

        val nodeKeyProps = result.listOfMaps("nodeKeyProperties")
        assertEquals(1, nodeKeyProps.size)
        assertEquals("#node1", nodeKeyProps[0].map("node").string("\$ref"))
        val nodeKeys = nodeKeyProps[0].listOfMaps("keyProperties")
        assertEquals(1, nodeKeys.size)
        assertEquals("#p1", nodeKeys[0].string("\$ref"))

        val relationshipKeyProps = result.listOfMaps("relationshipKeyProperties")
        assertEquals(1, relationshipKeyProps.size)
        assertEquals("#relationship1", relationshipKeyProps[0].map("relationship").string("\$ref"))
        val relKeys = relationshipKeyProps[0].listOfMaps("keyProperties")
        assertEquals(2, relKeys.size)
        assertEquals("#p2", relKeys[0].string("\$ref"))
    }

    @Test
    fun `convertSourceSchema handles complex foreign keys correctly`() {
        val input = schemaMapOf(
            "extensions" to schemaMapOf(
                "data_source_tables" to schemaMapOf(
                    "Orders" to schemaMapOf(
                        "source" to "SQL",
                        "foreign_keys" to schemaMapOf(
                            "fk_customer" to schemaMapOf(
                                "columns" to listOf("cust_id", "region_id"),
                                "references" to schemaMapOf(
                                    "table" to "Customers",
                                    "columns" to listOf("id", "reg_id")
                                )
                            )
                        )
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val tableSchemas = result
            .map("graphMappingRepresentation")
            .map("dataSourceSchema")
            .listOfMaps("tableSchemas")

        val fk = tableSchemas[0].listOfMaps("foreignKeys")[0]
        assertEquals("Customers", fk.string("referencedTable"))

        val fields = fk.listOfMaps("fields")
        assertEquals(2, fields.size)
        assertEquals("cust_id", fields[0].string("field"))
        assertEquals("id", fields[0].string("referencedField"))
        assertEquals("region_id", fields[1].string("field"))
        assertEquals("reg_id", fields[1].string("referencedField"))
    }

    @Test
    fun `convertFields transforms raw types to camelCase recommended types`() {
        val fieldsInput = listOf(
            schemaMapOf(
                "name" to "first_name",
                "suggested" to "STRING",
                "supported" to listOf(SchemaLiteral("STRING"), SchemaLiteral("INTEGER"))
            )
        )

        val fields = migration.convertFields(fieldsInput)

        assertEquals("first_name", fields[0].string("name"))
        assertEquals("string", fields[0].map("recommendedType").string("type"))
        val supported = fields[0].listOfMaps("supportedTypes")
        assertEquals("string", supported[0].string("type"))
        assertEquals("integer", supported[1].string("type"))
    }

    @Test
    fun `convertFields restores sample data from custom extensions`() {
        val fieldsInput = listOf(
            schemaMapOf("name" to "categoryID", "custom" to schemaMapOf("sample" to "1")),
            schemaMapOf("name" to "description")
        )

        val fields = migration.convertFields(fieldsInput)

        assertEquals("1", fields[0].string("sample"))
        assertFalse(fields[1].containsKey("sample"))
    }

    @Test
    fun `convertFields writes dimension field for vector types`() {
        val fieldsInput = listOf(
            schemaMapOf(
                "name" to "embeddingWithDim",
                "suggested" to "VECTOR<FLOAT>",
                "supported" to listOf(SchemaLiteral("VECTOR<FLOAT>"), SchemaLiteral("VECTOR<FLOAT32>")),
                "dimension" to 123
            ),
            schemaMapOf(
                "name" to "embedding",
                "suggested" to "VECTOR<FLOAT>",
                "supported" to listOf(SchemaLiteral("VECTOR<FLOAT>"))
            ),
            schemaMapOf(
                "name" to "stringWithDim",
                "suggested" to "STRING",
                "supported" to listOf(SchemaLiteral("STRING")),
                "dimension" to 8
            )
        )

        val fields = migration.convertFields(fieldsInput)

        assertEquals(123, fields[0].map("recommendedType").intOrNull("dimension"))
        assertEquals(
            listOf(123, 123),
            fields[0].listOfMaps("supportedTypes").map { it.intOrNull("dimension") }
        )

        assertFalse(fields[1].map("recommendedType").containsKey("dimension"))
        assertFalse(fields[2].map("recommendedType").containsKey("dimension"))
    }

    @Test
    fun `convertProperties writes dimension field for vector types`() {
        val properties = mapOf(
            "p1" to schemaMapOf("token" to "embeddingWithDim", "type" to "VECTOR<FLOAT>", "dimension" to 123),
            "p2" to schemaMapOf("token" to "embedding", "type" to "VECTOR<FLOAT>"),
            "p3" to schemaMapOf("token" to "stringWithDim", "type" to "LIST<STRING>", "dimension" to 4)
        )

        val converted = migration.convertProperties(properties).associateBy { it.string("\$id") }

        val vector = converted["p1"]!!.map("type")
        assertEquals("vector", vector.string("type"))
        assertEquals(123, vector.intOrNull("dimension"))

        assertFalse(converted["p2"]!!.map("type").containsKey("dimension"))
        assertFalse(converted["p3"]!!.map("type").containsKey("dimension"))
    }

    @Test
    fun `convertVisualisation transforms coordinates correctly`() {
        val schema = schemaMapOf(
            "nodes" to schemaMapOf(
                "node1" to schemaMapOf(
                    "extensions" to schemaMapOf("display" to schemaMapOf("x" to 100.23, "y" to 200.12))
                ),
                "node2" to schemaMapOf("labels" to schemaMapOf("identifier" to "NoDisplay"))
            )
        )

        val result = migration.convertVisualisation(schema)

        assertNotNull(result)
        val nodes = result.listOfMaps("nodes")
        assertEquals("node1", nodes[0].string("id"))
        assertEquals(100.23, nodes[0].map("position").string("x").toDouble())
        assertEquals(200.12, nodes[0].map("position").string("y").toDouble())
    }

    @Test
    fun `convertExtensions emits nodeKeyProperties as empty list when no node has key properties`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "properties" to schemaMapOf(
                        "p1" to schemaMapOf("mustExist" to false, "unique" to true), // mustExist — not a key
                        "p2" to schemaMapOf("mustExist" to true, "unique" to false) // not unique — not a key
                    )
                )
            ),
            "relationships" to schemaMapOf(
                "r1" to schemaMapOf(
                    "properties" to schemaMapOf(
                        "p1" to schemaMapOf("nullable" to true, "unique" to true), // nullable — not a key
                        "p2" to schemaMapOf("nullable" to false, "unique" to false) // not unique — not a key
                    )
                )
            )
        )

        val result = migration.convertExtensions(input)

        assertNotNull(result)
        assertTrue(result.containsKey("nodeKeyProperties"), "nodeKeyProperties key must always be present")
        assertEquals(emptyList(), result.listOfMapsOrNull("nodeKeyProperties"))
        assertNull(result.listOfMapsOrNull("relationshipKeyProperties"))
    }

    @Test
    fun `migrate graphMappingRepresentation is complete when tables key is absent`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf("labels" to schemaMapOf("identifier" to "Person"))
            )
        )

        val result = migration.migrate(input)
        val mapping = result.map("graphMappingRepresentation")

        assertTrue(mapping.containsKey("dataSourceSchema"), "dataSourceSchema must always be present")
        val dataSourceSchema = mapping.map("dataSourceSchema")
        assertTrue(dataSourceSchema["type"] is SchemaNull, "type should be null when no tables")
        assertEquals(emptyList(), dataSourceSchema.listOfMapsOrNull("tableSchemas"))

        assertTrue(mapping.containsKey("nodeMappings"), "nodeMappings must always be present")
        assertEquals(emptyList(), mapping.listOfMapsOrNull("nodeMappings"))

        assertTrue(mapping.containsKey("relationshipMappings"), "relationshipMappings must always be present")
        assertEquals(emptyList(), mapping.listOfMapsOrNull("relationshipMappings"))
    }

    @Test
    fun `migrate graphMappingRepresentation is complete when tables key is present but empty`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf("labels" to schemaMapOf("identifier" to "Person"))
            ),
            "extensions" to schemaMapOf("data_source_tables" to schemaMapOf()) // key present, but no table entries
        )

        val result = migration.migrate(input)
        val mapping = result.map("graphMappingRepresentation")

        assertTrue(mapping.containsKey("dataSourceSchema"), "dataSourceSchema must be present even for empty tables")
        val dataSourceSchema = mapping.map("dataSourceSchema")
        assertTrue(dataSourceSchema.containsKey("tableSchemas"), "tableSchemas must be present even when empty")
        assertEquals(emptyList(), dataSourceSchema.listOfMapsOrNull("tableSchemas"))

        assertEquals(emptyList(), mapping.listOfMapsOrNull("nodeMappings"))
        assertEquals(emptyList(), mapping.listOfMapsOrNull("relationshipMappings"))
    }

    @Test
    fun `migrate always includes configurations with idsToIgnore`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf("labels" to schemaMapOf("identifier" to "Person"))
            )
        )

        val result = migration.migrate(input)
        val dataModel = result

        assertTrue(dataModel.containsKey("configurations"), "configurations must always be present")
        val configurations = dataModel.map("configurations")
        assertTrue(configurations.containsKey("idsToIgnore"), "idsToIgnore must always be present")
        assertEquals(0, configurations.listOrNull("idsToIgnore")?.content?.size)
    }

    @Test
    fun `migrate emits expanded true for each converted table schema`() {
        val input = schemaMapOf(
            "extensions" to schemaMapOf(
                "data_source_tables" to schemaMapOf(
                    "users" to schemaMapOf(
                        "source" to "local",
                        "fields" to schemaMapOf(
                            "id" to schemaMapOf("name" to "id", "type" to "INTEGER")
                        )
                    )
                )
            )
        )

        val result = migration.migrate(input)
        val tableSchemas = result
            .map("graphMappingRepresentation")
            .map("dataSourceSchema")
            .listOfMaps("tableSchemas")

        assertEquals(1, tableSchemas.size)
        assertTrue(tableSchemas[0].containsKey("expanded"), "expanded must be present on each table schema")
        assertTrue(tableSchemas[0].bool("expanded"), "expanded should default to true")
    }

    @Test
    fun `migrate handles completely empty schema without crashing`() {
        val emptyInput = schemaMapOf("version" to "2.0")

        val result = migration.migrate(emptyInput)

        val graphSchema = result
            .map("graphSchemaRepresentation")
            .map("graphSchema")

        assertEquals(0, graphSchema.listOfMaps("nodeLabels").size)
        assertEquals(0, graphSchema.listOfMaps("relationshipTypes").size)
        assertEquals(0, graphSchema.listOfMaps("nodeObjectTypes").size)
        assertEquals(0, graphSchema.listOfMaps("relationshipObjectTypes").size)
        assertEquals(0, graphSchema.listOfMaps("constraints").size)
        assertEquals(0, graphSchema.listOfMaps("indexes").size)
    }

    @Test
    fun `migrate emits a flat data model without the wrapper fields`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "extensions" to schemaMapOf("display" to schemaMapOf("x" to 100.23, "y" to 200.12))
                )
            ),
            "description" to "a description"
        )

        val flat = migration.migrate(input)

        assertFalse(flat.containsKey("dataModel"), "flat output must not be wrapped")
        assertTrue(flat.containsKey("graphSchemaRepresentation"))
        assertTrue(flat.containsKey("graphMappingRepresentation"))
        assertFalse(flat.containsKey("visualisation"))
        assertFalse(flat.containsKey("description"))
    }

    @Test
    fun `wrapped migration envelopes the data model with visualisation and description`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "extensions" to schemaMapOf("display" to schemaMapOf("x" to 100.23, "y" to 200.12))
                )
            ),
            "description" to "a description"
        )

        val wrapped = GraphSpecDataModelWrappedMigration().migrate(input)

        assertEquals(Version.DATA_MODEL_WRAPPED_V3_0, wrapped.string("version"))
        assertEquals("a description", wrapped.string("description"))
        assertEquals("n1", wrapped.map("visualisation").listOfMaps("nodes").single().string("id"))
        val dataModel = wrapped.map("dataModel")
        assertTrue(dataModel.containsKey("graphSchemaRepresentation"))
        assertTrue(dataModel.containsKey("graphMappingRepresentation"))
        assertTrue(dataModel.containsKey("configurations"))
        assertFalse(dataModel.containsKey("visualisation"))
    }

    @Test
    fun `migrate builds constraint names from property and label when the graph spec has none`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING")),
                    "constraints" to schemaMapOf(
                        "uniq" to schemaMapOf("type" to "UNIQUE", "properties" to listOf("p1"))
                    )
                )
            ),
            "relationships" to schemaMapOf(
                "r1" to schemaMapOf(
                    "type" to "KNOWS",
                    "from" to schemaMapOf("node" to "n1"),
                    "to" to schemaMapOf("node" to "n1"),
                    "properties" to schemaMapOf("rp1" to schemaMapOf("name" to "since", "type" to "INTEGER")),
                    "constraints" to schemaMapOf(
                        "rel_uniq" to schemaMapOf("type" to "UNIQUE", "properties" to listOf("rp1"))
                    )
                )
            )
        )

        val graphSchema = migration.migrate(input).map("graphSchemaRepresentation").map("graphSchema")

        val constraints = graphSchema.listOfMaps("constraints")
        assertEquals("name_Person_uniq", constraints.first { it.string("entityType") == "node" }.string("name"))
        assertEquals("since_KNOWS_uniq", constraints.first { it.string("entityType") == "relationship" }.string("name"))
    }

    @Test
    fun `migrate builds index names from property and label when the graph spec has none`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING")),
                    "extensions" to schemaMapOf(
                        "indexes" to schemaMapOf(
                            "idx" to schemaMapOf("type" to "TEXT", "properties" to listOf("p1"))
                        )
                    )
                )
            ),
            "relationships" to schemaMapOf(
                "r1" to schemaMapOf(
                    "type" to "KNOWS",
                    "from" to schemaMapOf("node" to "n1"),
                    "to" to schemaMapOf("node" to "n1"),
                    "properties" to schemaMapOf("rp1" to schemaMapOf("name" to "since", "type" to "INTEGER")),
                    "extensions" to schemaMapOf(
                        "indexes" to schemaMapOf(
                            "rel_idx" to schemaMapOf("type" to "TEXT", "properties" to listOf("rp1"))
                        )
                    )
                )
            )
        )

        val graphSchema = migration.migrate(input).map("graphSchemaRepresentation").map("graphSchema")

        val indexes = graphSchema.listOfMaps("indexes")
        assertEquals("name_Person_text", indexes.first { it.string("entityType") == "node" }.string("name"))
        assertEquals("since_KNOWS_text", indexes.first { it.string("entityType") == "relationship" }.string("name"))
    }

    @Test
    fun `migrate keeps a name the graph spec provides`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING")),
                    "constraints" to schemaMapOf(
                        "uniq" to schemaMapOf("type" to "UNIQUE", "properties" to listOf("p1"), "name" to "my_name")
                    ),
                    "extensions" to schemaMapOf(
                        "indexes" to schemaMapOf(
                            "idx" to schemaMapOf("type" to "TEXT", "properties" to listOf("p1"), "name" to "my_index")
                        )
                    )
                )
            )
        )

        val graphSchema = migration.migrate(input).map("graphSchemaRepresentation").map("graphSchema")

        assertEquals("my_name", graphSchema.listOfMaps("constraints")[0].string("name"))
        assertEquals("my_index", graphSchema.listOfMaps("indexes")[0].string("name"))
    }

    @Test
    fun `migrate treats a blank name as absent`() {
        val input = schemaMapOf(
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person"),
                    "properties" to schemaMapOf("p1" to schemaMapOf("name" to "name", "type" to "STRING")),
                    "constraints" to schemaMapOf(
                        "uniq" to schemaMapOf("type" to "KEY", "properties" to listOf("p1"), "name" to "   ")
                    ),
                    "extensions" to schemaMapOf(
                        "indexes" to schemaMapOf(
                            "idx" to schemaMapOf("type" to "TEXT", "properties" to listOf("p1"), "name" to "")
                        )
                    )
                )
            )
        )

        val graphSchema = migration.migrate(input).map("graphSchemaRepresentation").map("graphSchema")

        assertEquals("name_Person_key", graphSchema.listOfMaps("constraints")[0].string("name"))
        assertEquals("name_Person_text", graphSchema.listOfMaps("indexes")[0].string("name"))
    }

    @Test
    fun `migrate fails for a node without a label`() {
        val missingLabels = schemaMapOf("nodes" to schemaMapOf("n" to schemaMapOf("description" to "no labels")))
        val missingIdentifier = schemaMapOf(
            "nodes" to schemaMapOf("n" to schemaMapOf("labels" to schemaMapOf("implied" to listOf("Entity"))))
        )

        listOf(missingLabels, missingIdentifier).forEach { input ->
            val failure = assertFailsWith<IllegalStateException> { migration.migrate(input) }
            assertTrue(failure.message!!.contains("has no label, set label or labels.identifier"))
        }
    }
}
