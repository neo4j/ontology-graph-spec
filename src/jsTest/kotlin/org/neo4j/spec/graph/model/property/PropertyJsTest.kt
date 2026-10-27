package org.neo4j.spec.graph.model.property

import org.neo4j.spec.graph.model.property.extension.toClass
import org.neo4j.spec.graph.model.property.extension.toJs
import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import org.neo4j.spec.graph.model.property.extension.PropertyExtensions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyJsTest : JsMappingTest<Property, PropertyJs>() {

    override fun createClass() = Property(
        type = Neo4jType.VECTOR_FLOAT,
        dimension = 8,
        mustExist = true,
        unique = true,
        key = true,
        extensions = PropertyExtensions(mutableMapOf("key1" to "val1")),
        name = "propertyName",
        aliases = mutableSetOf("alias1", "alias2"),
        reference = "ref",
        pattern = "^a.*",
        oneOf = mutableListOf("a", "b")
    )

    override fun toJs(k: Property): PropertyJs = k.toJs("propertyId")

    override fun toClass(js: PropertyJs): Property = js.toClass("parent", "propertyId")

    override fun verifyJsObject(jsObject: PropertyJs) {
        assertEquals("VECTOR<FLOAT>", jsObject.type)
        assertEquals(8, jsObject.dimension)
        assertTrue(jsObject.mustExist!!)
        assertTrue(jsObject.unique!!)
        assertTrue(jsObject.key!!)
        assertEquals<Any?>("val1", jsObject.extensions.custom["key1"])
        assertEquals("propertyId", jsObject.id)
        assertEquals("propertyName", jsObject.name)
        assertEquals(listOf("alias1", "alias2"), jsObject.aliases.toList())
        assertEquals("ref", jsObject.reference)
        assertEquals("^a.*", jsObject.pattern)
        assertEquals(2, jsObject.oneOf.size)
        assertEquals("a", jsObject.oneOf[0])
        assertEquals("b", jsObject.oneOf[1])
    }

}
