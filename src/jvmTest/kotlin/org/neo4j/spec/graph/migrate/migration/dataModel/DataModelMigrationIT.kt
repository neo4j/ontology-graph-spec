package org.neo4j.spec.graph.migrate.migration.dataModel

import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.codec.schema.SchemaElement
import org.neo4j.spec.graph.codec.schema.SchemaList
import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.codec.schema.SchemaNull
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.assertEquals

class DataModelMigrationIT {

    @TestFactory
    fun `Round trip`() =
        File(End2EndMigrationTest::class.java.getResource("prod-like")!!.path).listFiles()!!.map { file ->
            dynamicTest("dataset ${file.nameWithoutExtension}") {
                if (file.nameWithoutExtension == "two-nodes-same-name" || file.extension != "json") {
                    // TODO check if there's a good way of combining node labels that can be easily compared e2e
                    return@dynamicTest
                }
                val input = file.readText()
                val graphModel = GraphSpec.Json.decodeFromString(input, Type.DATA_MODEL)
                val graphSpec = GraphSpec {
                    json {
                        encodeDefaults = true
                        prettyPrint = true
                    }
                }
                val output = graphSpec.encodeToString(graphModel, Type.DATA_MODEL, Version.DATA_MODEL_V3_0)
                val expected = graphSpec.configuration.format.decodeFromString(input) as SchemaMap
                val actual = graphSpec.configuration.format.decodeFromString(output) as SchemaMap
                // Remove unused configurations
                removeDefaultSizes(actual)
                // Remove fields we don't mind losing in the round-trip
                expected.remove("configurations")
                actual.remove("configurations")
                expected
                    .map("graphMappingRepresentation")
                    .map("dataSourceSchema")
                    .listOfMaps("tableSchemas")
                    .forEach {
                        it.remove("rawType") // Shouldn't be at this level
                        it.remove("expanded")
                    }
                actual
                    .map("graphMappingRepresentation")
                    .map("dataSourceSchema")
                    .listOfMaps("tableSchemas")
                    .forEach {
                        it.remove("expanded")
                    }
                // Legacy data models have explicit nulls for the other entity's reference, they're now omitted IMP-1333
                removeNullEntityReferences(expected)
                // We need to handle #IMP-994 before comparing visualisations
                expected.remove("visualisation")

                assertEquals(
                    graphSpec.configuration.format.encodeToString(recursiveSort(expected)),
                    graphSpec.configuration.format.encodeToString(recursiveSort(actual))
                )
            }
        }

    private fun removeNullEntityReferences(expected: SchemaMap) {
        val graphSchema = expected.map("graphSchemaRepresentation").map("graphSchema")
        for (key in listOf("constraints", "indexes")) {
            graphSchema.listOfMapsOrNull(key)?.forEach { element ->
                for (reference in listOf("nodeLabel", "relationshipType")) {
                    if (element[reference] is SchemaNull) {
                        element.remove(reference)
                    }
                }
            }
        }
    }

    private fun removeDefaultSizes(actual: SchemaMap) {
        // Remove default size: -1
        actual
            .map("graphMappingRepresentation")
            .map("dataSourceSchema")
            .listOfMaps("tableSchemas")
            .forEach { schema ->
                schema.listOfMaps("fields").forEach {
                    it.remove("size")
                }
            }
    }


    private fun recursiveSort(element: SchemaElement): SchemaElement = when (element) {
        is SchemaMap -> {
            val sorted = sortedMapOf<String, SchemaElement>()
            for ((key, value) in element.content) {
                sorted[key] = recursiveSort(value)
            }
            SchemaMap(sorted)
        }
        is SchemaList -> SchemaList(element.content.map { recursiveSort(it) }.toMutableList())
        else -> element
    }
}
