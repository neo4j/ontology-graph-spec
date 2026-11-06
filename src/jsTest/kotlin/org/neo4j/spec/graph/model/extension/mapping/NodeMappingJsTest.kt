package org.neo4j.spec.graph.model.extension.mapping

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class NodeMappingJsTest : JsMappingTest<NodeMapping, NodeMappingJs>() {

    override fun createClass() = NodeMapping(
        node = "node",
        table = "table_name",
        properties = mutableMapOf("property" to PropertyMapping("field")),
        mode = MappingMode.CREATE,
        matchLabel = "label_name",
        key = mutableListOf("key1", "key2"),
        dependsOn = "other",
        id = "mapping-id"
    )

    override fun toJs(k: NodeMapping): NodeMappingJs = k.toJs()

    override fun toClass(js: NodeMappingJs): NodeMapping = js.toClass()

    override fun verifyJsObject(jsObject: NodeMappingJs) {
        assertEquals("NODE", jsObject.type)
        assertEquals("table_name", jsObject.table)
        assertJsEquals(propertyMappingJs("field"), jsObject.properties["property"])
        assertEquals("CREATE", jsObject.mode)
        assertEquals("label_name", jsObject.matchLabel)
        assertContentEquals(arrayOf("key1", "key2"), jsObject.key)
        assertEquals("other", jsObject.dependsOn)
        assertEquals("mapping-id", jsObject.id)
    }

}
