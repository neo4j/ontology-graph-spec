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
package org.neo4j.spec.graph.validate

import kotlin.test.Test
import kotlin.test.assertEquals

class PathOfTest {

    @Test
    fun `joins segments as an RFC 6901 pointer`() {
        assertEquals("/nodes/person/properties/name", pathOf("nodes", "person", "properties", "name"))
    }

    @Test
    fun `array indexes are plain segments`() {
        assertEquals(
            "/extensions/data_source_mappings/m0/1/key",
            pathOf("extensions", "data_source_mappings", "m0", 1, "key")
        )
    }

    @Test
    fun `dots and brackets in keys need no escaping`() {
        assertEquals("/nodes/weird.key[0]/labels", pathOf("nodes", "weird.key[0]", "labels"))
    }

    @Test
    fun `tildes and slashes are escaped`() {
        assertEquals("/nodes/a~0b/c~1d", pathOf("nodes", "a~b", "c/d"))
    }
}
