/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.spec.graph.migrate.migration.dataModel

import org.neo4j.spec.graph.codec.schema.schemaMapOf
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DataModelWrapperTest {

    @Test
    fun `wrap moves wrapper fields into the envelope`() {
        val flat = schemaMapOf(
            "version" to "3.0.0",
            "graphSchemaRepresentation" to schemaMapOf(),
            "visualisation" to schemaMapOf("nodes" to emptyList<Any>()),
            "description" to "a description"
        )

        val wrapped = DataModelWrapper.wrap(flat)

        assertEquals(Version.DATA_MODEL_WRAPPED_V3_0, wrapped.string("version"))
        assertEquals("a description", wrapped.string("description"))
        assertTrue(wrapped.containsKey("visualisation"))
        val dataModel = wrapped.map("dataModel")
        assertEquals("3.0.0", dataModel.string("version"))
        assertTrue(dataModel.containsKey("graphSchemaRepresentation"))
        assertFalse(dataModel.containsKey("visualisation"))
        assertFalse(dataModel.containsKey("description"))
    }

    @Test
    fun `wrap omits missing wrapper fields`() {
        val wrapped = DataModelWrapper.wrap(schemaMapOf("version" to "3.0.0"))

        assertEquals(setOf("version", "dataModel"), wrapped.keys)
    }

    @Test
    fun `unwrap hoists wrapper fields into the data model`() {
        val wrapped = schemaMapOf(
            "version" to "5.0.0",
            "dataModel" to schemaMapOf("version" to "2.4.0"),
            "visualisation" to schemaMapOf("nodes" to emptyList<Any>()),
            "description" to "a description"
        )

        val flat = DataModelWrapper.unwrap(wrapped)

        assertEquals("2.4.0", flat.string("version"))
        assertEquals("a description", flat.string("description"))
        assertTrue(flat.containsKey("visualisation"))
        assertFalse(flat.containsKey("dataModel"))
    }

    @Test
    fun `unwrap leaves flat models untouched`() {
        val flat = schemaMapOf("version" to "3.0.0")

        assertSame(flat, DataModelWrapper.unwrap(flat))
    }

    @Test
    fun `wrap then unwrap round trips`() {
        val flat = schemaMapOf(
            "version" to "3.0.0",
            "visualisation" to schemaMapOf("nodes" to emptyList<Any>()),
            "description" to "a description"
        )
        val expected = flat.toString()

        assertEquals(expected, DataModelWrapper.unwrap(DataModelWrapper.wrap(flat)).toString())
    }
}
