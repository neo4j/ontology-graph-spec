package org.neo4j.spec.graph.model.extension.source

import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import org.neo4j.spec.graph.model.property.Neo4jType
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TableColumnJsTest : JsMappingTest<TableColumn, TableColumnJs>() {

    override fun createClass() = TableColumn(
        type = "field_type",
        size = 10,
        suggested = Neo4jType.VECTOR_FLOAT,
        supported = listOf(Neo4jType.VECTOR_FLOAT, Neo4jType.VECTOR_FLOAT32),
        nullable = false,
        dimension = 8,
        custom = mutableMapOf("key1" to "val1"),
        name = "Field name"
    )

    override fun toJs(k: TableColumn): TableColumnJs = k.toJs()

    override fun toClass(js: TableColumnJs): TableColumn = js.toClass()

    override fun verifyJsObject(jsObject: TableColumnJs) {
        assertEquals("field_type", jsObject.type)
        assertEquals(10, jsObject.size)
        assertEquals("VECTOR<FLOAT>", jsObject.suggested)
        assertContentEquals(arrayOf("VECTOR<FLOAT>", "VECTOR<FLOAT32>"), jsObject.supported)
        assertEquals(false, jsObject.nullable)
        assertEquals(8, jsObject.dimension)
        assertEquals<Any?>("val1", jsObject.custom["key1"])
        assertEquals("Field name", jsObject.name)
    }

}
