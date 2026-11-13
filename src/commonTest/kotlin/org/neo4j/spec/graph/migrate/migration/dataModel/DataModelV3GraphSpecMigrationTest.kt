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

import org.neo4j.spec.graph.codec.schema.schemaMapOf
import org.neo4j.spec.graph.codec.schema.toSchemaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataModelV3GraphSpecMigrationTest {

    private val migration = DataModelV3GraphSpecMigration()

    @Test
    fun `visualisation throws error if node reference is missing`() {
        val nodes = mutableMapOf("nodeA" to schemaMapOf("id" to "nodeA"))
        val schema = schemaMapOf(
            "visualisation" to schemaMapOf(
                "nodes" to listOf(
                    schemaMapOf(
                        "id" to "nonExistentNode",
                        "position" to mapOf("x" to 10.1, "y" to 20.2)
                    )
                )
            )
        )

        assertFailsWith<IllegalStateException>("Unknown node nonExistentNode") {
            migration.convertVisualisations(schema, nodes)
        }
    }

    @Test
    fun `visualisation moves positions into node extensions`() {
        val nodes = mutableMapOf("nodeA" to schemaMapOf("id" to "nodeA"))
        val schema = schemaMapOf(
            "visualisation" to schemaMapOf(
                "nodes" to listOf(
                    schemaMapOf(
                        "id" to "nodeA",
                        "position" to mapOf("x" to 10.1234, "y" to 20.54321)
                    )
                )
            )
        )

        migration.convertVisualisations(schema, nodes)
        val display = nodes["nodeA"]!!.map("extensions").map("display")
        assertEquals("10.1234", display.string("x"))
        assertEquals("20.54321", display.string("y"))
    }

    @Test
    fun `gather partitions constraints correctly`() {
        val graphSchema = schemaMapOf(
            "constraints" to listOf(
                schemaMapOf(
                    "entityType" to "node",
                    "nodeLabel" to mapOf("\$ref" to "#L1"),
                    "constraintType" to "uniqueness"
                ),
                schemaMapOf(
                    "entityType" to "relationship",
                    "relationshipType" to mapOf("\$ref" to "#R1"),
                    "constraintType" to "propertyExistence"
                )
            )
        )

        val (nodeCons, relCons) = migration.gatherWithNames(graphSchema, "constraints")

        assertTrue(nodeCons.containsKey("L1"))
        assertTrue(relCons.containsKey("R1"))
        assertEquals("uniqueness", nodeCons["L1"]?.first()?.string("constraintType"))
    }

    @Test
    fun `migrateNodes handles multiple labels with identifier and implied`() {
        val nodeLabels = listOf(
            schemaMapOf("\$id" to "L1", "token" to "Actor", "properties" to emptyList<Any>()),
            schemaMapOf("\$id" to "L2", "token" to "Person", "properties" to emptyList<Any>()),
            schemaMapOf("\$id" to "L3", "token" to "Human", "properties" to emptyList<Any>())
        )
        val graphSchema = schemaMapOf(
            "nodeLabels" to nodeLabels,
            "nodeObjectTypes" to listOf(
                schemaMapOf(
                    "\$id" to "nodeObj",
                    "labels" to listOf(mapOf("\$ref" to "#L1"), mapOf("\$ref" to "#L2"), mapOf("\$ref" to "#L3"))
                )
            )
        )

        val nodes = migration.migrateNodes(graphSchema, emptyMap(), emptyMap())

        val node = nodes["nodeObj"]
        assertNotNull(node)
        val labels = node.map("labels")
        assertEquals("Actor", labels.string("identifier"))
        assertEquals(listOf("Person", "Human").toSchemaElement(), labels.list("implied"))
    }

    @Test
    fun `migrateNodes transforms labels properties and keys`() {
        val labelId = "lbl1"
        val nodeLabels = schemaMapOf(
            labelId to schemaMapOf(
                "\$id" to labelId,
                "token" to "Person",
                "properties" to listOf(
                    schemaMapOf(
                        "\$id" to "prop1",
                        "token" to "name",
                        "type" to mapOf("type" to "string"),
                        "nullable" to false,
                        "description" to "prop description"
                    )
                )
            )
        )

        val graphSchema = schemaMapOf(
            "nodeLabels" to listOf(nodeLabels[labelId]),
            "nodeObjectTypes" to listOf(
                schemaMapOf(
                    "\$id" to "nodeObj1",
                    "labels" to listOf(mapOf("\$ref" to "#$labelId")),
                    "description" to "a description"
                )
            )
        )

        val nodes = migration.migrateNodes(graphSchema, emptyMap(), emptyMap())

        val migratedNode = nodes["nodeObj1"]
        assertNotNull(migratedNode)
        assertEquals("a description", migratedNode.stringOrNull("description"))
        val labels = migratedNode.map("labels")
        assertEquals("Person", labels.stringOrNull("identifier"))
        val props = migratedNode.map("properties")
        assertNotNull(props["prop1"])
        val prop = props.map("prop1")
        assertEquals("STRING", prop.string("type"))
        assertEquals("prop description", prop.stringOrNull("description"))
    }

    @Test
    fun `convertIndexes resolves types and handles names`() {
        val indexes = mapOf(
            "label1" to listOf(
                schemaMapOf(
                    "\$id" to "i:0",
                    "name" to "custom_idx",
                    "indexType" to "range",
                    "properties" to listOf(mapOf("\$ref" to "#p1"))
                ),
                schemaMapOf(
                    "\$id" to "i:1",
                    "indexType" to "text",
                    "properties" to listOf(mapOf("\$ref" to "#p2"))
                )
            )
        )

        val result = migration.convertIndexes(indexes, "label1", "Person", mapOf("p1" to "prop1", "p2" to "prop2"))
        assertNotNull(result)

        // First index uses custom name
        val idx1 = result["i:0"]
        assertNotNull(idx1)
        assertEquals("RANGE", idx1.string("type"))
        assertEquals(listOf("Person").toSchemaElement(), idx1.list("labels"))
        assertEquals("custom_idx", idx1.string("name"))

        // Second index gets a name built from its property and label
        val idx2 = result["i:1"]
        assertNotNull(idx2)
        assertEquals("TEXT", idx2.string("type"))
        assertEquals(listOf("p2").toSchemaElement(), idx2.list("properties"))
        assertEquals("prop2_Person_text", idx2.string("name"))
    }

    @Test
    fun `indexType throws error on unknown index type`() {
        val index = schemaMapOf(
            "indexType" to "UNKNOWN_TYPE",
            "name" to "my_idx",
            "path" to "some.path"
        )

        assertFailsWith<IllegalStateException> {
            migration.convertIndexes(mapOf("L1" to listOf(index)), "L1", "Label", emptyMap())
        }
    }

    @Test
    fun `convertConstraints throws error for multi-property propertyType constraints`() {
        val constraints = mapOf(
            "L1" to listOf(
                schemaMapOf(
                    "constraintType" to "propertyType",
                    "name" to "bad_constraint",
                    "properties" to listOf(mapOf("\$ref" to "#p1"), mapOf("\$ref" to "#p2"))
                )
            )
        )

        val error = assertFailsWith<IllegalStateException> {
            migration.convertConstraints(constraints, "L1", "Person", emptyMap())
        }

        assertEquals("Type constraints not supported on multiple properties.", error.message)
    }

    @Test
    fun `convertConstraints handles property refs and unique names`() {
        val constraints = mapOf(
            "label1" to listOf(
                schemaMapOf(
                    "\$id" to "c:1",
                    "name" to "constraint1",
                    "constraintType" to "uniqueness",
                    "properties" to listOf(mapOf("\$ref" to "#p1"), mapOf("\$ref" to "#p2"))
                )
            )
        )

        val result = migration.convertConstraints(
            constraints,
            "label1",
            "Person",
            mapOf(
                "p1" to "prop1",
                "p2" to "prop2"
            )
        )

        assertNotNull(result)
        val constraint = result["c:1"]
        assertNotNull(constraint)
        assertEquals("UNIQUE", constraint.string("type"))
        assertEquals(listOf("p1", "p2").toSchemaElement(), constraint.list("properties"))
        assertEquals("constraint1", constraint.string("name"))
    }

    @Test
    fun `convertConstraints builds a name when the data model has none`() {
        val constraints = mapOf(
            "label1" to listOf(
                schemaMapOf(
                    "\$id" to "c:1",
                    "name" to "",
                    "constraintType" to "uniqueness",
                    "properties" to listOf(mapOf("\$ref" to "#p1"), mapOf("\$ref" to "#p2"))
                )
            )
        )

        val result = migration.convertConstraints(
            constraints,
            "label1",
            "Person",
            mapOf(
                "p1" to "prop1",
                "p2" to "prop2"
            )
        )

        assertNotNull(result)
        assertEquals("prop1_prop2_Person_uniq", result["c:1"]?.string("name"))
    }

    @Test
    fun `migrateRelationships handles full transformation`() {
        val relTypeRef = "relType1"
        val graphSchema = schemaMapOf(
            "relationshipTypes" to listOf(
                schemaMapOf(
                    "\$id" to relTypeRef,
                    "token" to "FOLLOWS",
                    "properties" to listOf(
                        schemaMapOf(
                            "\$id" to "p1",
                            "token" to "since",
                            "type" to mapOf("type" to "datetime"),
                            "description" to "prop description"
                        )
                    )
                )
            ),
            "relationshipObjectTypes" to listOf(
                schemaMapOf(
                    "\$id" to "relObj1",
                    "type" to mapOf("\$ref" to "#$relTypeRef"),
                    "from" to mapOf("\$ref" to "#nodeA"),
                    "to" to mapOf("\$ref" to "#nodeB"),
                    "description" to "a description"
                )
            )
        )

        val constraints = mapOf(
            relTypeRef to listOf(
                schemaMapOf(
                    "\$id" to "c1",
                    "name" to "constraint",
                    "constraintType" to "propertyExistence",
                    "properties" to listOf(mapOf("\$ref" to "#p1"))
                )
            )
        )

        val result = migration.migrateRelationships(
            graphSchema,
            constraints,
            emptyMap()
        )

        assertTrue(result.containsKey("relObj1"))
        val rel = result["relObj1"]
        assertNotNull(rel)
        assertEquals("FOLLOWS", rel.string("type"))
        assertEquals("nodeA", rel.map("from").string("node"))
        assertEquals("a description", rel.string("description"))
        val prop = rel.map("properties").map("p1")
        assertEquals("ZONED DATETIME", prop.string("type"))
        assertEquals("prop description", prop.string("description"))
        assertNotNull(rel.map("constraints")["c1"])
    }

    @Test
    fun `migrateRelationships moves indexes into relationship extensions`() {
        val graphSchema = schemaMapOf(
            "relationshipTypes" to listOf(
                schemaMapOf(
                    "\$id" to "relType1",
                    "token" to "FOLLOWS",
                    "properties" to listOf(
                        schemaMapOf("\$id" to "p1", "token" to "since", "type" to mapOf("type" to "datetime"))
                    )
                )
            ),
            "relationshipObjectTypes" to listOf(
                schemaMapOf(
                    "\$id" to "relObj1",
                    "type" to mapOf("\$ref" to "#relType1"),
                    "from" to mapOf("\$ref" to "#nodeA"),
                    "to" to mapOf("\$ref" to "#nodeB")
                )
            )
        )
        val indexes = mapOf(
            "relType1" to listOf(
                schemaMapOf(
                    "\$id" to "i:0",
                    "name" to "since_idx",
                    "indexType" to "range",
                    "properties" to listOf(mapOf("\$ref" to "#p1"))
                )
            )
        )

        val rel = migration.migrateRelationships(graphSchema, emptyMap(), indexes)["relObj1"]
        assertNotNull(rel)
        assertFalse(rel.containsKey("indexes"))
        val index = rel.map("extensions").map("indexes").map("i:0")
        assertEquals("RANGE", index.string("type"))
        assertEquals("since_idx", index.string("name"))
        assertEquals(listOf("p1").toSchemaElement(), index.list("properties"))
    }

    @Test
    fun `convertProperties normalizes types to uppercase`() {
        val labels = listOf(
            schemaMapOf(
                "properties" to listOf(
                    schemaMapOf(
                        "\$id" to "p1",
                        "token" to "name",
                        "type" to mapOf("type" to "string")
                    ),
                    schemaMapOf(
                        "\$id" to "p2",
                        "token" to "age",
                        "type" to mapOf("type" to "Integer")
                    )
                )
            )
        )

        val result = migration.convertProperties(labels)

        assertEquals("STRING", result["p1"]?.string("type"))
        assertEquals("INTEGER", result["p2"]?.string("type"))
    }

    @Test
    fun `convertProperties converts array and vector types to their graph spec type`() {
        val labels = listOf(
            schemaMapOf(
                "properties" to listOf(
                    schemaMapOf(
                        "\$id" to "arrProp",
                        "token" to "tags",
                        "type" to mapOf("type" to "array", "items" to mapOf("type" to "string"))
                    ),
                    schemaMapOf(
                        "\$id" to "vecProp",
                        "token" to "embedding",
                        "type" to mapOf("type" to "vector", "items" to mapOf("type" to "float"))
                    ),
                    schemaMapOf(
                        "\$id" to "vecPropWithDim",
                        "token" to "embedding2",
                        "type" to mapOf("type" to "vector", "items" to mapOf("type" to "float"), "dimension" to 123)
                    ),
                    schemaMapOf(
                        "\$id" to "arrPropWithDim",
                        "token" to "tags2",
                        "type" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "dimension" to 4)
                    )
                )
            )
        )

        val result = migration.convertProperties(labels)

        assertEquals("LIST<STRING>", result["arrProp"]?.string("type"))
        assertEquals("VECTOR<FLOAT>", result["vecProp"]?.string("type"))
        assertEquals("VECTOR<FLOAT>", result["vecPropWithDim"]?.string("type"))
        assertEquals("LIST<STRING>", result["arrPropWithDim"]?.string("type"))
        // dimension only relevant for vector properties
        assertFalse(result["arrProp"]!!.containsKey("dimension"))
        assertFalse(result["vecProp"]!!.containsKey("dimension"))
        assertEquals(123, result["vecPropWithDim"]!!.intOrNull("dimension"))
        assertFalse(result["arrPropWithDim"]!!.containsKey("dimension"))
    }

    @Test
    fun `relationshipMappings joins object types and tokens correctly`() {
        val input = schemaMapOf(
            "graphSchemaRepresentation" to mapOf(
                "graphSchema" to mapOf(
                    "relationshipObjectTypes" to listOf(
                        schemaMapOf(
                            "\$id" to "obj1",
                            "type" to mapOf("\$ref" to "#type1"),
                            "from" to mapOf("\$ref" to "#A"),
                            "to" to mapOf("\$ref" to "#B")
                        )
                    ),
                    "relationshipTypes" to listOf(
                        schemaMapOf("\$id" to "type1", "token" to "WORKS_AT")
                    )
                )
            ),
            "graphMappingRepresentation" to mapOf(
                "relationshipMappings" to listOf(
                    schemaMapOf(
                        "relationship" to mapOf("\$ref" to "#obj1"),
                        "tableName" to "WORKS_IN",
                        "fromMappings" to mapOf("#propA" to "COL_A"), // Tests prefix removal
                        "toMappings" to mapOf("#propB" to "COL_B"),
                        "propertyMappings" to emptyList<Any>()
                    )
                )
            )
        )

        val result = migration.relationshipMappings(input, emptyMap())

        val mapping = result.first()
        assertEquals("obj1", mapping.string("relationship"))
        assertEquals("WORKS_IN", mapping.string("table"))

        val startProps = mapping.map("from_node").map("properties")
        assertTrue(startProps.containsKey("propA"))
        assertEquals("COL_A", startProps.map("propA").string("column"))
    }

    @Test
    fun `relationshipMappings throws error when relationship object type is missing`() {
        val schema = schemaMapOf(
            "graphSchemaRepresentation" to mapOf(
                "graphSchema" to mapOf(
                    "relationshipObjectTypes" to emptyList<Any>(),
                    "relationshipTypes" to emptyList()
                )
            ),
            "graphMappingRepresentation" to mapOf(
                "relationshipMappings" to listOf(
                    schemaMapOf("relationship" to mapOf("\$ref" to "#missingRel"))
                )
            )
        )

        assertFailsWith<IllegalStateException>("Relationship missingRel not found") {
            migration.relationshipMappings(schema, emptyMap())
        }
    }

    @Test
    fun `nodeMappings transforms correctly`() {
        val input = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "nodeMappings" to listOf(
                    schemaMapOf(
                        "node" to mapOf("\$ref" to "#node1"),
                        "tableName" to "USERS_TABLE",
                        "propertyMappings" to listOf(
                            schemaMapOf(
                                "property" to mapOf("\$ref" to "#prop1"),
                                "fieldName" to "USER_NAME"
                            )
                        )
                    )
                )
            )
        )

        val result = migration.nodeMappings(input, emptyMap())

        assertEquals(1, result.size)
        val mapping = result.first()
        assertEquals("node1", mapping.string("node"))
        assertEquals("USERS_TABLE", mapping.string("table"))
        assertEquals("USER_NAME", mapping.map("properties").map("prop1").string("column"))
    }

    @Test
    fun `migrateTables handles foreign keys`() {
        val inputSchema = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "type" to "JDBC",
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "users",
                            "fields" to listOf(
                                schemaMapOf("name" to "id", "rawType" to "INT")
                            ),
                            "foreignKeys" to listOf(
                                schemaMapOf(
                                    "referencedTable" to "accounts",
                                    "fields" to listOf(
                                        mapOf("field" to "account_id", "referencedField" to "id")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val tables = migration.migrateTables(inputSchema)

        val userTable = tables["users"]
        assertNotNull(userTable)
        val fks = userTable.mapOfMaps("foreign_keys")
        assertEquals(1, fks.size)
        val firstFK = fks.values.first()
        assertEquals("accounts", firstFK.map("references").string("table"))
    }

    @Test
    fun `migrateTables excludes empty foreign_keys and primary_keys`() {
        val inputSchema = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "empty_table",
                            "fields" to listOf(mapOf("name" to "id")),
                            "primaryKeys" to emptyList<String>(),
                            "foreignKeys" to emptyList<Any>()
                        )
                    )
                )
            )
        )

        val tables = migration.migrateTables(inputSchema)
        val table = tables["empty_table"]!!

        assertFalse(table.containsKey("primary_keys"), "primary_keys should be omitted if empty")
        assertFalse(table.containsKey("foreign_keys"), "foreign_keys should be omitted if empty")
    }

    @Test
    fun `migrateTables converts array and vector types to their graph spec type`() {
        val inputSchema = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "film",
                            "fields" to listOf(
                                schemaMapOf(
                                    "name" to "special_features",
                                    "rawType" to "SET",
                                    "recommendedType" to mapOf("type" to "string"),
                                    "supportedTypes" to listOf(
                                        mapOf("type" to "string"),
                                        mapOf("type" to "array", "items" to mapOf("type" to "string"))
                                    )
                                ),
                                schemaMapOf(
                                    "name" to "embedding",
                                    "rawType" to "VECTOR",
                                    "recommendedType" to mapOf("type" to "vector", "items" to mapOf("type" to "float")),
                                    "supportedTypes" to listOf(
                                        mapOf("type" to "vector", "items" to mapOf("type" to "float")),
                                        mapOf("type" to "vector", "items" to mapOf("type" to "float32"))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val columns = migration.migrateTables(inputSchema)["film"]!!.listOfMaps("columns").associateBy {
            it.string("name")
        }

        val arrayField = columns["special_features"]!!
        assertEquals("STRING", arrayField.string("suggested"))
        assertEquals(
            listOf("STRING", "LIST<STRING>"),
            arrayField.list("supported").map { (it as org.neo4j.spec.graph.codec.schema.SchemaLiteral).string }
        )

        val vectorField = columns["embedding"]!!
        assertEquals("VECTOR<FLOAT>", vectorField.string("suggested"))
        assertEquals(
            listOf("VECTOR<FLOAT>", "VECTOR<FLOAT32>"),
            vectorField.list("supported").map { (it as org.neo4j.spec.graph.codec.schema.SchemaLiteral).string }
        )
    }

    @Test
    fun `migrateTables copies sample data to custom extensions`() {
        val inputSchema = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "type" to "local",
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "categories.csv",
                            "fields" to listOf(
                                schemaMapOf("name" to "categoryID", "sample" to "1"),
                                schemaMapOf("name" to "description")
                            )
                        )
                    )
                )
            )
        )

        val columns = migration.migrateTables(inputSchema)["categories.csv"]!!.listOfMaps("columns")

        assertEquals("1", columns[0].map("custom").string("sample"))
        assertFalse(columns[1].containsKey("custom"), "custom should be omitted without sample data")
    }

    @Test
    fun `migrateTables processes vector dimension`() {
        val inputSchema = schemaMapOf(
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "document",
                            "fields" to listOf(
                                schemaMapOf(
                                    "name" to "embeddingWithDim",
                                    "rawType" to "VECTOR",
                                    "recommendedType" to mapOf(
                                        "type" to "vector",
                                        "items" to mapOf("type" to "float"),
                                        "dimension" to 123
                                    ),
                                    "supportedTypes" to listOf(
                                        mapOf(
                                            "type" to "vector",
                                            "items" to mapOf("type" to "float"),
                                            "dimension" to 123
                                        )
                                    )
                                ),
                                // no recommendedType, so the dimension comes from the supported types
                                schemaMapOf(
                                    "name" to "supportedOnly",
                                    "rawType" to "VECTOR",
                                    "supportedTypes" to listOf(
                                        mapOf(
                                            "type" to "vector",
                                            "items" to mapOf("type" to "float32"),
                                            "dimension" to 456
                                        )
                                    )
                                ),
                                schemaMapOf(
                                    "name" to "embedding",
                                    "rawType" to "VECTOR",
                                    "recommendedType" to mapOf("type" to "vector", "items" to mapOf("type" to "float"))
                                ),
                                schemaMapOf(
                                    "name" to "stringWithDim",
                                    "rawType" to "VARCHAR",
                                    "recommendedType" to mapOf("type" to "string", "dimension" to 8)
                                )
                            )
                        )
                    )
                )
            )
        )

        val fields = migration.migrateTables(inputSchema)["document"]!!.listOfMaps("columns").associateBy {
            it.string("name")
        }

        assertEquals(123, fields["embeddingWithDim"]?.intOrNull("dimension"))
        assertEquals(456, fields["supportedOnly"]?.intOrNull("dimension"))
        assertFalse(fields["embedding"]!!.containsKey("dimension"))
        assertFalse(fields["stringWithDim"]!!.containsKey("dimension"))
    }

    @Test
    fun `migrate supports source-schema-only input without graph schema`() {
        val input = schemaMapOf(
            "version" to "3.0.0",
            "description" to "a description",
            "graphMappingRepresentation" to mapOf(
                "dataSourceSchema" to schemaMapOf(
                    "type" to "cloud",
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "users",
                            "fields" to listOf(schemaMapOf("name" to "id", "rawType" to "INT"))
                        )
                    )
                )
            )
        )

        val result = migration.migrate(input)

        assertNotNull(result.map("extensions").mapOfMaps("data_source_tables")["users"])
        assertTrue(result.mapOfMaps("nodes").isEmpty(), "nodes should be present but empty")
        assertTrue(result.mapOfMaps("relationships").isEmpty(), "relationships should be present but empty")
        assertFalse(
            result.map("extensions").containsKey("data_source_mappings"),
            "mappings should be omitted when empty"
        )
        assertTrue(result.containsKey("description"), "description should be present but empty")
    }

    @Test
    fun `aliases cardinality pattern and one_of are migrated`() {
        val schema = schemaMapOf(
            "graphSchemaRepresentation" to mapOf(
                "graphSchema" to mapOf(
                    "nodeLabels" to listOf(
                        mapOf(
                            "\$id" to "nl:A",
                            "token" to "A",
                            "properties" to listOf(
                                mapOf(
                                    "\$id" to "p:A.x",
                                    "token" to "x",
                                    "type" to mapOf("type" to "string"),
                                    "description" to "an x",
                                    "aliases" to listOf("ex", "why"),
                                    "reference" to "ref:p",
                                    "pattern" to "^[A-Z].*$",
                                    "one_of" to listOf("D1", "D2")
                                )
                            )
                        ),
                        mapOf("\$id" to "nl:B", "token" to "B", "properties" to emptyList<Any>())
                    ),
                    "relationshipTypes" to listOf(
                        mapOf("\$id" to "rt:R", "token" to "R", "properties" to emptyList<Any>())
                    ),
                    "nodeObjectTypes" to listOf(
                        mapOf("\$id" to "n:A", "labels" to listOf(mapOf("\$ref" to "#nl:A")), "aliases" to listOf("Aa"), "reference" to "ref:n"),
                        mapOf("\$id" to "n:B", "labels" to listOf(mapOf("\$ref" to "#nl:B")))
                    ),
                    "relationshipObjectTypes" to listOf(
                        mapOf(
                            "\$id" to "r:R",
                            "type" to mapOf("\$ref" to "#rt:R"),
                            "from" to mapOf("\$ref" to "#n:A", "count" to 1),
                            "to" to mapOf("\$ref" to "#n:B", "minCount" to 0, "maxCount" to 5),
                            "aliases" to listOf("RR"),
                            "reference" to "ref:r"
                        )
                    )
                )
            ),
            "graphMappingRepresentation" to mapOf("nodeMappings" to emptyList<Any>())
        )

        val result = migration.migrate(schema)

        val node = result.map("nodes").map("n:A")
        assertEquals(listOf("Aa"), node.list("aliases").map { it.toString() })
        val prop = node.map("properties").map("p:A.x")
        assertEquals(listOf("ex", "why"), prop.list("aliases").map { it.toString() })
        assertEquals("^[A-Z].*$", prop.string("pattern"))
        assertEquals("ref:p", prop.string("reference"))
        assertEquals("ref:n", node.string("reference"))
        assertEquals("ref:r", result.map("relationships").map("r:R").string("reference"))
        assertEquals(listOf("D1", "D2"), prop.list("one_of").map { it.toString() })
        val rel = result.map("relationships").map("r:R")
        assertEquals(listOf("RR"), rel.list("aliases").map { it.toString() })
        assertEquals("n:A", rel.map("from").string("node"))
        assertEquals(1, rel.map("from").int("count"))
        assertEquals(0, rel.map("to").int("min_count"))
        assertEquals(5, rel.map("to").int("max_count"))
        assertEquals(null, rel.map("to").literalOrNull("count"))
    }
}
