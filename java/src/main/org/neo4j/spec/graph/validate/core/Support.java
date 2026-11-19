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
package org.neo4j.spec.graph.validate.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.model.Neo4jType;
import org.neo4j.spec.graph.validate.Issue;

/** Helpers shared by the core validators. */
final class Support {

    private Support() {}

    /** The length of a string in Unicode code points. */
    static int length(String value) {
        return value.codePointCount(0, value.length());
    }

    /** Adds an issue to {@code issues} for every entry that repeats an earlier one, at {@code path[index]}. */
    static void repeats(List<Issue> issues, String code, String what, String path, List<?> entries) {
        Set<Object> seen = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            if (!seen.add(entry)) {
                String value = entry instanceof Neo4jType ? ((Neo4jType) entry).value() : String.valueOf(entry);
                issues.add(Issue.of(
                        code,
                        "Repeated " + what + " '" + value + "' at " + path + "[" + i + "]",
                        path + "[" + i + "]",
                        Map.of("value", value)));
            }
        }
    }
}
