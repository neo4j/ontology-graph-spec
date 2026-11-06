package org.neo4j.spec.graph.model.node

import org.neo4j.spec.graph.model.node.extension.NodeConstraintExtensions
import org.neo4j.spec.graph.model.node.extension.toClass
import org.neo4j.spec.graph.model.node.extension.toJs
import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import org.neo4j.spec.graph.model.type.ConstraintType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeConstraintJsTest : JsMappingTest<NodeConstraint, NodeConstraintJs>() {

    override fun createClass() = NodeConstraint(
        type = ConstraintType.KEY,
        properties = mutableListOf("property_1", "property_2"),
        extensions = NodeConstraintExtensions(
            mutableMapOf("key1" to "val1")
        )
    )

    override fun toJs(k: NodeConstraint): NodeConstraintJs = k.toJs()

    override fun toClass(js: NodeConstraintJs): NodeConstraint = js.toClass()

    override fun verifyJsObject(jsObject: NodeConstraintJs) {
        assertEquals("KEY", jsObject.type)
        assertEquals(2, jsObject.properties.size)
        assertTrue(jsObject.properties.contains("property_1"))
        assertTrue(jsObject.properties.contains("property_2"))
        assertEquals<Any?>("val1", jsObject.extensions.custom["key1"])
    }

}
