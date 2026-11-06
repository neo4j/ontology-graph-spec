package org.neo4j.spec.graph.model.node

import org.neo4j.spec.graph.model.node.extension.NodeDisplay
import org.neo4j.spec.graph.model.node.extension.NodeExtensions
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.node.extension.nodeIndexJs
import org.neo4j.spec.graph.model.node.extension.toClass
import org.neo4j.spec.graph.model.node.extension.toJs
import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.property.propertyJs
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.IndexType
import kotlin.test.assertEquals

class NodeJsTest : JsMappingTest<Node, NodeJs>() {

    override fun createClass() = Node(
        labels = Labels("label"),
        properties = mutableMapOf("prop" to Property(Neo4jType.STRING, name = "propertyName")),
        constraints = mutableMapOf("constraint" to NodeConstraint(ConstraintType.EXISTS, mutableListOf("prop"))),
        extensions = NodeExtensions(
            custom = mutableMapOf("key1" to "val1"),
            indexes = mutableMapOf("index" to NodeIndex(IndexType.RANGE, mutableListOf("label"), mutableListOf("prop"))),
            display = NodeDisplay(1.0, 2.0, "#FF8800")
        ),
        aliases = mutableListOf("alias1", "alias2"),
        reference = "node-ref"
    )

    override fun toJs(k: Node): NodeJs = k.toJs("nodeId")

    override fun toClass(js: NodeJs): Node = js.toClass("nodeId")

    override fun verifyJsObject(jsObject: NodeJs) {
        assertEquals("label", jsObject.labels.identifier)
        assertJsEquals(propertyJs("STRING", id = "prop", name = "propertyName"), jsObject.properties["prop"])
        assertJsEquals(nodeConstraintJs("EXISTS", arrayOf("prop")), jsObject.constraints["constraint"])
        assertJsEquals(nodeIndexJs("RANGE", arrayOf("label"), arrayOf("prop")), jsObject.extensions.indexes["index"])
        assertEquals<Any?>("val1", jsObject.extensions.custom["key1"])
        assertEquals(1.0, jsObject.extensions.display?.x)
        assertEquals(2.0, jsObject.extensions.display?.y)
        assertEquals("#FF8800", jsObject.extensions.display?.colour)
        assertEquals("nodeId", jsObject.id)
        assertEquals(listOf("alias1", "alias2"), jsObject.aliases.toList())
        assertEquals("node-ref", jsObject.reference)
    }

}
