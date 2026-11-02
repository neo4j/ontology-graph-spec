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
package org.neo4j.spec.graph.codec.schema

import kotlin.jvm.JvmOverloads

class SchemaList @JvmOverloads constructor(val content: MutableList<SchemaElement>, path: String = "") :
    SchemaElement,
    MutableList<SchemaElement> by content {

    override var parent: SchemaElement? = null
    override var identifier: String = path

    init {
        content.forEachIndexed { index, element -> adopt(element, "[$index]") }
    }

    override fun repath(newPath: String): SchemaList = reroot(newPath)

    override fun equals(other: Any?): Boolean = content == other

    override fun hashCode(): Int = content.hashCode()

    override fun toString(): String = content.joinToString(prefix = "[", postfix = "]", separator = ",")
}

fun schemaListOf(vararg elements: Any?): SchemaList = SchemaList(elements.map { it.toSchemaElement() }.toMutableList())
