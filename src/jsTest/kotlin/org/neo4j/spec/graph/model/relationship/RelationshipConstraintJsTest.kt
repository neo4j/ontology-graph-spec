package org.neo4j.spec.graph.model.relationship

import org.neo4j.spec.graph.model.relationship.extension.RelationshipConstraintExtensions
import org.neo4j.spec.graph.model.relationship.extension.toClass
import org.neo4j.spec.graph.model.relationship.extension.toJs
import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import org.neo4j.spec.graph.model.type.ConstraintType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipConstraintJsTest : JsMappingTest<RelationshipConstraint, RelationshipConstraintJs>() {

    override fun createClass() = RelationshipConstraint(
        type = ConstraintType.UNIQUE,
        properties = mutableListOf("property_1", "property_2"),
        extensions = RelationshipConstraintExtensions(
            mutableMapOf("key1" to "val1")
        )
    )

    override fun toJs(k: RelationshipConstraint): RelationshipConstraintJs = k.toJs()

    override fun toClass(js: RelationshipConstraintJs): RelationshipConstraint = js.toClass()

    override fun verifyJsObject(jsObject: RelationshipConstraintJs) {
        assertEquals("UNIQUE", jsObject.type)
        assertEquals(2, jsObject.properties.size)
        assertTrue(jsObject.properties.contains("property_1"))
        assertTrue(jsObject.properties.contains("property_2"))
        assertEquals<Any?>("val1", jsObject.extensions.custom["key1"])
    }

}
