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
package org.neo4j.spec.graph.validate.duplicate

import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.jsonPointerOf

/**
 * Finds the repeated entries of an ordered collection that must not repeat, such as aliases or constraint
 * properties. Shared by every validator covering a list which used to be a set, so they all report in the same way.
 *
 * Returns one issue for each entry that repeats an earlier one, located by its index in the list at [path], so the
 * first occurrence is left alone and the repeats are what get flagged.
 *
 * @param code the issue code
 * @param kind what the entries are, e.g. "alias", for the message
 * @param owner what the list belongs to, e.g. "node 'Person'", for the message
 * @param details extra details to attach to each issue; the repeated value is added as `value`
 */
internal fun duplicateEntryIssues(
    code: String,
    kind: String,
    owner: String,
    values: List<Any>,
    vararg path: Any,
    details: Map<String, String> = emptyMap()
): List<Issue> {
    val seen = mutableSetOf<Any>()
    val issues = mutableListOf<Issue>()
    for ((index, value) in values.withIndex()) {
        if (!seen.add(value)) {
            issues.add(
                Issue(
                    code = code,
                    message = "Duplicate $kind '$value' on $owner",
                    path = jsonPointerOf(*path, index),
                    details = details + ("value" to value.toString())
                )
            )
        }
    }
    return issues
}
