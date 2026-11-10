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
    val path: String? = null, // e.g. "nodes.Person.labels"
    val details: Map<String, String> = emptyMap()
)

/**
 * What kind of thing an [Issue] concerns. Serialized as the lowercase name in [Issue.details];
 * TypeScript consumers mirror this as a string union.
 */
@JsExport
enum class IssueKeyType {
    MODEL,
    NODE,
    RELATIONSHIP,
    PROPERTY,
    INDEX,
    CONSTRAINT,
    TABLE,
    COLUMN,
    MAPPING
}

/**
 * Machine anchor for an issue: `keyType` says what kind of thing the issue concerns and `key` is its
 * key in the model (entity key, property id, column name, index or constraint id). Unlike [Issue.path],
 * this is safe to read programmatically. Entries without a value are left out, so a missing `key` means
 * the flagged thing has no key of its own - the [Issue.path] array position locates it instead.
 * `dependsOn` carries the referenced id for issues about a broken dependency.
 */
fun issueDetails(keyType: IssueKeyType? = null, key: String? = null, dependsOn: String? = null): Map<String, String> =
    buildMap {
        keyType?.let { put("keyType", it.name.lowercase()) }
        key?.let { put("key", it) }
        dependsOn?.let { put("dependsOn", it) }
    }
