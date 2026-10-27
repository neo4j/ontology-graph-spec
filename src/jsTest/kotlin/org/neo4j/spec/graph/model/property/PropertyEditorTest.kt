package org.neo4j.spec.graph.model.property

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyEditorTest {

    private fun property() = propertyJs(name = "p", id = "p")

    @Test
    fun testOneOfDefaultsEmpty() {
        assertTrue(property().oneOf.isEmpty())
    }

    @Test
    fun testAddOneOf() {
        val property = property()

        PropertyEditor.addOneOf(property, "a")
        PropertyEditor.addOneOf(property, "b")

        assertEquals(listOf("a", "b"), property.oneOf.toList())
    }

    @Test
    fun testRemoveOneOfByIndex() {
        val property = property()
        PropertyEditor.addOneOf(property, "a")
        PropertyEditor.addOneOf(property, "b")
        PropertyEditor.addOneOf(property, "c")

        PropertyEditor.removeOneOf(property, 1)

        assertEquals(listOf("a", "c"), property.oneOf.toList())
    }

    @Test
    fun testRemoveOneOfOutOfRangeIsIgnored() {
        val property = property()
        PropertyEditor.addOneOf(property, "a")

        PropertyEditor.removeOneOf(property, 5)
        PropertyEditor.removeOneOf(property, -1)

        assertEquals(1, property.oneOf.size)
    }

    @Test
    fun testAddAliasIgnoresDuplicates() {
        val property = property()

        PropertyEditor.addAlias(property, "a")
        PropertyEditor.addAlias(property, "b")
        PropertyEditor.addAlias(property, "a")

        assertEquals(listOf("a", "b"), property.aliases.toList())
    }

    @Test
    fun testRemoveAlias() {
        val property = property()
        PropertyEditor.addAlias(property, "a")
        PropertyEditor.addAlias(property, "b")

        PropertyEditor.removeAlias(property, "a")

        assertEquals(listOf("b"), property.aliases.toList())
    }

    @Test
    fun testSetReferenceAndPattern() {
        val property = property()

        PropertyEditor.setReference(property, "ref")
        PropertyEditor.setPattern(property, "^x$")

        assertEquals("ref", property.reference)
        assertEquals("^x$", property.pattern)
    }
}
