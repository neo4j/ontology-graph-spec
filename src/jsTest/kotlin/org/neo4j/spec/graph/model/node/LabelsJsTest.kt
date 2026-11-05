package org.neo4j.spec.graph.model.node

import org.neo4j.spec.graph.model.node.extension.LabelsExtensions
import org.neo4j.spec.graph.model.node.extension.toClass
import org.neo4j.spec.graph.model.node.extension.toJs
import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import kotlin.test.assertEquals

class LabelsJsTest : JsMappingTest<Labels, LabelsJs>() {

    override fun createClass() = Labels(
        identifier = "test-id",
        implied = mutableListOf("a", "b"),
        optional = mutableListOf("c"),
        extensions = LabelsExtensions(mutableMapOf("key1" to "val1"))
    )

    override fun toJs(k: Labels): LabelsJs = k.toJs()

    override fun toClass(js: LabelsJs): Labels = js.toClass()

    override fun verifyJsObject(jsObject: LabelsJs) {
        assertEquals("test-id", jsObject.identifier)
        assertEquals(2, jsObject.implied.size)
        assertEquals("a", jsObject.implied[0])
        assertEquals<Any?>("val1", jsObject.extensions.custom["key1"])
    }

}
