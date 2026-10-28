package org.neo4j.spec.graph.migrate.migration.dataModel

import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.neo4j.spec.graph.resourceAsString
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class End2EndMigrationTest {

    private val fixtures = listOf(
        "adventureworks-sales.json",
        "chinook.json",
        "dvd-rental.json",
        "flights.json",
        "ldbc.json",
        "northwind.json",
        "pandc.json",
        "vector-embeddings.json"
    )

    @TestFactory
    fun `round trip examples`() = fixtures.map { name ->
        dynamicTest(name.removeSuffix(".json")) {
            val input = End2EndMigrationTest::class.resourceAsString("prod-like/$name")
            val expected = End2EndMigrationTest::class.resourceAsString("prod-like/${name.replace(".json", ".yaml")}")

            val model = GraphSpec.Json.decodeFromString(input, Type.DATA_MODEL)

            val output = GraphSpec.Yaml.encodeToString(model)

            assertEquals(expected, output)
        }
    }


    @TestFactory
    fun `graph-spec round trip examples`() = fixtures.map { name ->
        dynamicTest(name.removeSuffix(".json")) {
            val input = End2EndMigrationTest::class.resourceAsString("prod-like/$name")
            val json = GraphSpec.Json.encodeToString(GraphSpec.Json.decodeFromString(input, Type.DATA_MODEL))

            val model = GraphSpec.Json.decodeFromString(json, Type.GRAPH_SPEC)
            val output = GraphSpec.Json.encodeToString(model)

            assertEquals(json, output)
        }
    }

    @TestFactory
    fun `decoding a graph-spec model with an unsupported version fails`() = mapOf(
        GraphSpec.Json to """{"schema":"https://neo4j.com/docs/ontology-graph-spec/3.0.0/schema.json","version":"1","id":"test"}""",
        GraphSpec.Yaml to """
            schema: "https://neo4j.com/docs/ontology-graph-spec/3.0.0/schema.json"
            version: "1"
            id: "test"
        """.trimIndent(),
    ).map { (graphSpec, input) ->
        dynamicTest(graphSpec.configuration.format.javaClass.toString()) {
            val failure = assertFailsWith<IllegalStateException> {
                graphSpec.decodeFromString(input, Type.GRAPH_SPEC)
            }

            assertTrue(
                failure.message!!.contains("Unsupported migration from graph_spec:3.0.0 to graph_spec:1.0.0"),
                "unexpected message: ${failure.message}"
            )
        }
    }

    @TestFactory
    fun `decoding a graph-spec model without a schema fails`() = mapOf(
        GraphSpec.Json to """{"version":"1","id":"test"}""",
        GraphSpec.Yaml to """
            version: "1"
            id: "test"
        """.trimIndent(),
    ).map { (graphSpec, input) ->
        dynamicTest(graphSpec.configuration.format.javaClass.toString()) {
            val failure = assertFailsWith<Exception> {
                graphSpec.decodeFromString(input, Type.GRAPH_SPEC)
            }

            assertTrue(failure.message!!.contains("schema"), "unexpected message: ${failure.message}")
        }
    }

    @Test
    fun `wrapped round trip keeps visualisation in the envelope`() {
        val input = End2EndMigrationTest::class.resourceAsString("prod-like/northwind.json")
        val model = GraphSpec.Json.decodeFromString(input, Type.DATA_MODEL)

        val wrapped = GraphSpec.Json.encodeToString(model, Type.DATA_MODEL_WRAPPED, Version.DATA_MODEL_V3_0)

        val envelope = GraphSpec.Json.configuration.format.decodeFromString(wrapped) as SchemaMap
        assertEquals(Version.DATA_MODEL_WRAPPED_V3_0, envelope.string("version"))
        assertTrue(envelope.map("visualisation").listOfMaps("nodes").isNotEmpty())
        val dataModel = envelope.map("dataModel")
        assertEquals(Version.DATA_MODEL_V3_0, dataModel.string("version"))
        assertFalse(dataModel.containsKey("visualisation"))

        val decoded = GraphSpec.Json.decodeFromString(wrapped, Type.DATA_MODEL_WRAPPED)
        assertEquals(GraphSpec.Json.encodeToString(model), GraphSpec.Json.encodeToString(decoded))
    }
}
