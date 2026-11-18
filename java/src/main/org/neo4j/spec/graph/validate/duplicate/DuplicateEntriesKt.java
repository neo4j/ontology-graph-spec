// Generated from org/neo4j/spec/graph/validate/duplicate/DuplicateEntries.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.duplicate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;
import org.neo4j.spec.graph.validate.Issue;

public final class DuplicateEntriesKt {
    private DuplicateEntriesKt() {
    }

    /**
     * Finds the repeated entries of an ordered collection that must not repeat, such as aliases or constraint
     * properties. Shared by every validator covering a list which used to be a set, so they all report in the same way.
     *
     * Returns one issue for each entry that repeats an earlier one, located by its index in the list at {@code path}, so the
     * first occurrence is left alone and the repeats are what get flagged.
     */
    public static List<Issue> duplicateEntryIssues(String code, String kind, String owner,
            List<?> values, String path, Map<String, String> details) {
        Set<Object> seen = new LinkedHashSet<Object>();
        List<Issue> issues = new ArrayList<Issue>();
        for (var item : Kt.withIndex(values)) {
            var index = item.getIndex();
            var value = item.getValue();
            if (!seen.add(value)) {
                issues.add(new Issue(code, "Duplicate " + kind + " '" + value + "' on " + owner, path + "[" + index + "]", Kt.plusEntry(details, new Pair<>("value", String.valueOf(value)))));
            }
        }
        return issues;
    }

    public static List<Issue> duplicateEntryIssues(String code, String kind, String owner,
            List<?> values, String path) {
        return DuplicateEntriesKt.duplicateEntryIssues(code, kind, owner, values, path, Kt.<String, String>emptyMap());
    }
}
