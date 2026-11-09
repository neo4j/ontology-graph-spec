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

import kotlinx.serialization.Serializable
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

@Serializable
@JsExport
data class Issue @JvmOverloads constructor(
    val code: String,
    val message: String,
    val path: String? = null, // RFC 6901 JSON Pointer, e.g. "/nodes/Person/labels"
    val details: Map<String, String> = emptyMap()
)

/**
 * Machine anchor for an issue: `keyType` says what kind of thing the issue concerns
 * (model, node, relationship, property, index, constraint, table, column or mapping) and `key` is its
 * key in the model (entity key, property id, column name, index or constraint id). Unlike [Issue.path],
 * this is safe to read programmatically.
 */
fun issueDetails(keyType: String, key: String): Map<String, String> = mapOf("keyType" to keyType, "key" to key)

/**
 * Builds an RFC 6901 JSON Pointer for [Issue.path]. Segments are escaped (`~` to `~0`, `/` to `~1`), so
 * keys containing dots, slashes or brackets stay unambiguous to machine readers.
 */
fun pathOf(vararg segments: Any): String =
    segments.joinToString("") { "/" + it.toString().replace("~", "~0").replace("/", "~1") }
