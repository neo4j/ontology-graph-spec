package org.neo4j.spec.graph.model.node.extension

import org.neo4j.spec.graph.model.node.toClass
import org.neo4j.spec.graph.model.node.toJs
import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import org.neo4j.spec.graph.model.index.VectorIndexOption
import org.neo4j.spec.graph.model.index.VectorIndexOptionJs
import org.neo4j.spec.graph.model.type.IndexType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeIndexJsTest : JsMappingTest<NodeIndex, NodeIndexJs>() {

    override fun createClass() = NodeIndex(
        type = IndexType.TEXT,
        labels = mutableListOf("node_label"),
        properties = mutableListOf("property_1", "property_2"),
        options = VectorIndexOption(
            similarityFunction = "test",
        ),
        custom = mutableMapOf(
            "key1" to "val1"
        )
    )

    override fun toJs(k: NodeIndex): NodeIndexJs = k.toJs()

    override fun toClass(js: NodeIndexJs): NodeIndex = js.toClass()

    override fun verifyJsObject(jsObject: NodeIndexJs) {
        assertEquals("TEXT", jsObject.type)
        val labels = jsObject.labels
        assertEquals(1, labels.size)
        assertTrue(labels.contains("node_label"))
        assertEquals(2, jsObject.properties.size)
        assertTrue(jsObject.properties.contains("property_1"))
        assertTrue(jsObject.properties.contains("property_2"))
        assertJsEquals("test", (jsObject.options as? VectorIndexOptionJs)?.similarityFunction)
        assertEquals<Any?>("val1", jsObject.custom["key1"])
    }

}
