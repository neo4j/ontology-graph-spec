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
package org.neo4j.spec.graph.validate.duplicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.validate.Issue;

public final class DuplicateEntries {
    private DuplicateEntries() {}

    /**
     * Finds the repeated entries of an ordered collection that must not repeat, such as aliases or constraint
     * properties. Shared by every validator covering a list which used to be a set, so they all report in the same way.
     *
     * Returns one issue for each entry that repeats an earlier one, located by its index in the list at {@code path}, so the
     * first occurrence is left alone and the repeats are what get flagged.
     */
    public static List<Issue> duplicateEntryIssues(
            String code, String kind, String owner, List<?> values, String path, Map<String, String> details) {
        Set<Object> seen = new LinkedHashSet<>();
        List<Issue> issues = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            var value = values.get(index);
            if (!seen.add(value)) {
                Map<String, String> issueDetails = new LinkedHashMap<>(details);
                issueDetails.put("value", String.valueOf(value));
                issues.add(new Issue(
                        code,
                        "Duplicate " + kind + " '" + value + "' on " + owner,
                        path + Issue.jsonPointerOf(index),
                        issueDetails));
            }
        }
        return issues;
    }

    public static List<Issue> duplicateEntryIssues(
            String code, String kind, String owner, List<?> values, String path) {
        return DuplicateEntries.duplicateEntryIssues(code, kind, owner, values, path, Collections.emptyMap());
    }
}
