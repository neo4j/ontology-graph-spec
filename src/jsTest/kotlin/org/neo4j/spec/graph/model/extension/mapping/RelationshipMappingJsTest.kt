package org.neo4j.spec.graph.model.extension.mapping

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RelationshipMappingJsTest : JsMappingTest<RelationshipMapping, RelationshipMappingJs>() {

    override fun createClass() = RelationshipMapping(
        relationship = "relationshipId",
        table = "table_name",
        fromNode = TargetMapping(label = "from_label"),
        toNode = TargetMapping(label = "to_label"),
        properties = mutableMapOf("prop" to PropertyMapping("field")),
        mode = MappingMode.MERGE,
        matchLabel = "matchLabel",
        key = mutableListOf("key"),
        dependsOn = "other",
        id = "mapping-id"
    )

    override fun toJs(k: RelationshipMapping): RelationshipMappingJs = k.toJs()

    override fun toClass(js: RelationshipMappingJs): RelationshipMapping = js.toClass()

    override fun verifyJsObject(jsObject: RelationshipMappingJs) {
        assertEquals("RELATIONSHIP", jsObject.type)
        assertEquals("table_name", jsObject.table)
        assertEquals("from_label", jsObject.fromNode.label)
        assertEquals("to_label", jsObject.toNode.label)
        assertJsEquals(propertyMappingJs("field"), jsObject.properties["prop"])
        assertEquals("MERGE", jsObject.mode)
        assertEquals("matchLabel", jsObject.matchLabel)
        assertContentEquals(arrayOf("key"), jsObject.key)
        assertEquals("other", jsObject.dependsOn)
        assertEquals("mapping-id", jsObject.id)
    }

}
