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

class SchemaNull @JvmOverloads constructor(path: String = "") : SchemaPrimitive() {
    override var parent: SchemaElement? = null
    override var identifier: String = path

    override fun equals(other: Any?): Boolean = other is SchemaNull

    override fun hashCode(): Int = toString().hashCode()

    override fun repath(newPath: String): SchemaNull = reroot(newPath)

    override fun toString() = "null"

    override val string: String = "null"

    override val isString: Boolean
        get() = false
}
