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
package org.neo4j.spec.graph.codec.format

import org.neo4j.spec.graph.codec.schema.SchemaElement
import kotlin.jvm.JvmOverloads

class YamlPrintOptions @JvmOverloads constructor(
    val indent: Int = 2,
    val alwaysQuoteStrings: Boolean = false,
    val inlinePaths: Set<String> = emptySet()
) {
    private val matchers = inlinePaths.map { pattern -> pattern to Regex(patternToRegex(pattern)) }

    fun shouldInline(element: SchemaElement): Boolean {
        if (matchers.isEmpty()) return false
        val path = element.path
        return matchers.any { (pattern, regex) -> pattern == path || regex.matches(path) }
    }

    private fun patternToRegex(pattern: String): String = buildString {
        append("^")
        var i = 0
        while (i < pattern.length) {
            if (pattern.startsWith("**", i)) {
                // match any characters across multiple hierarchy levels
                append(".*")
                i += 2
            } else if (pattern.startsWith("*", i)) {
                // matches single key/index level
                append("[^.]+")
                i += 1
            } else {
                val char = pattern[i]
                // Escape standard regex special characters
                if ("\\^$.|?+()[]{}".contains(char)) {
                    append('\\').append(char)
                } else {
                    append(char)
                }
                i += 1
            }
        }
        append("$")
    }
}
