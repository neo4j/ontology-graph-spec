package org.neo4j.spec.graph

import js.objects.recordOf
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.GraphModelEditor
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import org.neo4j.spec.graph.model.node.extension.nodeDisplayJs
import org.neo4j.spec.graph.model.node.extension.nodeExtensionsJs
import org.neo4j.spec.graph.model.graphModelJs
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.labelsJs
import org.neo4j.spec.graph.model.node.nodeJs
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.property.propertyJs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsEdgeCaseTests {
    @Test
    fun `encodeToString should handle LIST<STRING>`() {
        val plainSpec = graphModelJs(
            version = "1",
            id = "test",
            nodes = recordOf(
                "n:1" to nodeJs(
                    labels = labelsJs("Movie"),
                    properties = recordOf(
                        "p:n1:1" to propertyJs("LIST<STRING>", name = "genres", id = "p:n1:1"),
                    ),
                    id = "n:1",
                    extensions = nodeExtensionsJs(display = nodeDisplayJs(0.0, 0.0))
                )
            )
        )
        val model = GraphModelEditor.model(plainSpec)

        val encoded = GraphSpec.Json.encodeToString(model, "data_model", "3.0.0")

        val expected = """
            "type": {
                "type": "array",
                "items": {
                    "type": "string"
                }
            },
        """.trimIndent()
        assertTrue(encoded.replace(" ", "").contains(expected.replace(" ", "")))
    }

    @Test
    fun `encodeToString shouldn't drop fields`() {
        val graphSpec = GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf(
                "PersonNode" to Node(
                    labels = Labels("Person"),
                    properties = mutableMapOf("born" to Property(Neo4jType.INTEGER, mustExist = true)),
                )
            )
        )
        graphSpec.longhand()
        val plain = GraphModelEditor.plain(graphSpec)
        val model = GraphModelEditor.model(plain)
        val encoded = GraphSpec.Json.encodeToString(model, Type.DATA_MODEL, Version.DATA_MODEL_V3_0)

        assertTrue(encoded.contains("\"token\": \"born\""))
        assertTrue(encoded.contains("\"nullable\": false"))

        val decoded = GraphSpec.Json.decodeFromString(encoded, Type.DATA_MODEL)
        val node = decoded.nodes.values.single()
        assertEquals("Person", node.labels.identifier)
        assertEquals("born", node.properties.values.single().name)
    }

    @Test
    fun `encodeToString should be able to encode a graph spec that is using shorthand label syntax to data_model`() {
        val graphSpec = GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf(
                "Person" to Node(
                    label = "Person",
                    properties = mutableMapOf("name" to Property(Neo4jType.STRING)),
                )
            )
        )
        val encoded = GraphSpec.Json.encodeToString(graphSpec, Type.DATA_MODEL, Version.DATA_MODEL_V3_0)
        assertTrue(encoded.contains("\"token\": \"Person\""))
    }

    @Test
    fun `encodeToString shouldn't drop field names`() {
        val string = """
       {
          "version": "3.0.0",
          "dataModel": {
            "version": "3.0.0",
            "graphSchemaRepresentation": {
              "version": "1.0.0",
              "graphSchema": {
                "nodeLabels": [
                  {
                    "${'$'}id": "nl:1",
                    "token": "Test",
                    "properties": []
                  }
                ],
                "nodeObjectTypes": [
                  {
                    "${'$'}id": "n:1",
                    "labels": [
                      {
                        "${'$'}ref": "#nl:1"
                      }
                    ]
                  }
                ],
                "relationshipTypes": [],
                "relationshipObjectTypes": [],
                "indexes": [],
                "constraints": []
              }
            },
            "graphSchemaExtensionsRepresentation": {
              "nodeKeyProperties": [],
              "relationshipKeyProperties": []
            },
            "graphMappingRepresentation": {
              "dataSourceSchema": {
                "type": "local",
                "tableSchemas": [
                  {
                    "name": "categories.csv",
                    "expanded": true,
                    "fields": [
                      {
                        "name": "categoryID",
                        "sample": "1",
                        "recommendedType": {
                          "type": "integer"
                        }
                      }
                    ],
                    "primaryKeys": [],
                    "foreignKeys": []
                  }
                ]
              },
              "nodeMappings": [
              ],
              "relationshipMappings": []
            },
            "configurations": {
              "idsToIgnore": []
            }
          }
        }
        """.trimIndent()
        val dataModel = GraphSpec.Json.decodeFromString(string, Type.DATA_MODEL)
        val plain = GraphModelEditor.plain(dataModel)
        val filled = GraphModelEditor.model(plain)
        val encoded = GraphSpec.Json.encodeToString(filled, Type.DATA_MODEL, Version.DATA_MODEL_V3_0)
        val decoded = GraphSpec.Json.decodeFromString(encoded, Type.DATA_MODEL)
        assertNotNull(decoded)
    }

}

