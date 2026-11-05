package org.neo4j.spec.graph.model.relationship

import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex
import org.neo4j.spec.graph.model.relationship.extension.relationshipIndexJs
import org.neo4j.spec.graph.model.relationship.extension.toClass
import org.neo4j.spec.graph.model.relationship.extension.toJs
import org.neo4j.spec.graph.model.extension.mapping.JsMappingTest
import org.neo4j.spec.graph.model.relationship.extension.RelationshipExtensions
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.property.propertyJs
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.IndexType
import kotlin.test.assertEquals

class RelationshipJsTest : JsMappingTest<Relationship, RelationshipJs>() {

    override fun createClass() = Relationship(
        type = "RELATIONSHIP_TYPE",
        from = RelationshipTarget("from_node"),
        to = RelationshipTarget("to_node"),
        properties = mutableMapOf("prop" to Property(Neo4jType.STRING, name = "property_name")),
        constraints = mutableMapOf("constraint" to RelationshipConstraint(ConstraintType.KEY, mutableListOf("prop"))),
        extensions = RelationshipExtensions(
            custom = mutableMapOf("key1" to "val1"),
            indexes = mutableMapOf("index" to RelationshipIndex(IndexType.POINT, mutableListOf("prop")))
        ),
        aliases = mutableListOf("alias1", "alias2"),
        reference = "rel-ref"
    )

    override fun toJs(k: Relationship): RelationshipJs = k.toJs("relationshipId")

    override fun toClass(js: RelationshipJs): Relationship = js.toClass("relationshipId")

    override fun verifyJsObject(jsObject: RelationshipJs) {
        assertEquals("RELATIONSHIP_TYPE", jsObject.type)
        assertEquals("from_node", jsObject.from.node)
        assertEquals("to_node", jsObject.to.node)
        assertJsEquals(propertyJs("STRING", id = "prop", name = "property_name"), jsObject.properties["prop"])
        assertJsEquals(relationshipConstraintJs("KEY", arrayOf("prop")), jsObject.constraints["constraint"])
        assertJsEquals(relationshipIndexJs("POINT", arrayOf("prop")), jsObject.extensions.indexes["index"])
        assertEquals<Any?>("val1", jsObject.extensions.custom["key1"])
        assertEquals("relationshipId", jsObject.id)
        assertEquals(listOf("alias1", "alias2"), jsObject.aliases.toList())
        assertEquals("rel-ref", jsObject.reference)
    }

}
