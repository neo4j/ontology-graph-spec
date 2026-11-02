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
@file:JvmName("SchemaElements")
@file:JvmMultifileClass

package org.neo4j.spec.graph.codec.schema

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * A format agnostic AST used for transforms and migrations.
 *
 * Elements know their location in the tree via a link to their [parent] plus their own
 * [identifier]. [path] is derived from that chain on demand to keep the memory footprint
 * down to a minimum and ensure that [repath] doesn't require a deep copy of the subtree.
 *
 * @see [codec.format.Format] for conversions
 * @see [migrate.Migration] for usages
 */
sealed interface SchemaElement {
    /** The element that owns this one, or null when this element is a tree root. */
    var parent: SchemaElement?

    /** This element's location relative to [parent]: a key for maps, `[i]` for lists. */
    var identifier: String

    val path: String get() {
        val prefix = parent?.path
        return when {
            prefix.isNullOrEmpty() -> identifier
            identifier.startsWith('[') -> prefix + identifier
            else -> "$prefix.$identifier"
        }
    }

    /** Re-roots this element at [newPath]. Descendants follow via their parent links. */
    fun repath(newPath: String): SchemaElement

    override fun toString(): String
}

/** Re-roots [this] at [newPath] in place. Descendants follow via their parent links. */
internal fun <T : SchemaElement> T.reroot(newPath: String): T {
    parent = null
    identifier = newPath
    return this
}

/** Adopts [child] so its [SchemaElement.path] resolves through this element. */
internal fun <T : SchemaElement> SchemaElement.adopt(child: T, identifier: String): T {
    child.parent = this
    child.identifier = identifier
    return child
}

fun Any?.toSchemaElement(path: String = ""): SchemaElement = when (this) {
    null -> SchemaNull(path)
    is SchemaElement -> this.repath(path)
    is Map<*, *> -> SchemaMap(
        entries.associateTo(mutableMapOf()) { (k, v) -> k.toString() to v.toSchemaElement() },
        path
    )
    is Iterable<*> -> SchemaList(mapTo(mutableListOf()) { it.toSchemaElement() }, path)
    is String -> SchemaLiteral(toString(), path, isString = true)
    else -> SchemaLiteral(toString(), path, isString = false)
}
