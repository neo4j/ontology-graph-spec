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
package org.neo4j.spec.graph.validate.consistent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks the shape of indexes suits their type. {@code TEXT}, {@code POINT} and {@code VECTOR} indexes need exactly one
 * property, {@code RANGE} and {@code FULLTEXT} at least one, and {@code LOOKUP} none. On nodes, {@code RANGE},
 * {@code TEXT}, {@code POINT} and {@code VECTOR} indexes need exactly one label, {@code FULLTEXT} at least one, and
 * {@code LOOKUP} none; and every label must be one of the node's labels. Reports
 * {@code invalid_node_index_property_count} and {@code invalid_relationship_index_property_count} at the index's
 * {@code properties}, {@code invalid_node_index_label_count} at its {@code labels}, each with the {@code type} and
 * {@code count}, and {@code node_index_label_not_on_node} at the label, with its {@code value}.
 */
public final class IndexShapeValidator implements Validator {

    static final IndexShapeValidator INSTANCE = new IndexShapeValidator();

    private IndexShapeValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Schema.Index index : Schema.indexes(model)) {
            count(issues, index, "property", "properties", index.properties().size(), propertyRule(index.type()));
            if (index.isNode()) {
                count(issues, index, "label", "labels", index.labels().size(), labelRule(index.type()));
                for (int i = 0; i < index.labels().size(); i++) {
                    String label = index.labels().get(i);
                    if (!index.ownerLabels().contains(label)) {
                        issues.add(Issue.of(
                                "node_index_label_not_on_node",
                                "The index '" + index.id() + "' is on the label '" + label + "', which isn't a label of"
                                        + " the node '" + index.ownerId() + "'",
                                index.path() + ".labels[" + i + "]",
                                Map.of("value", label)));
                    }
                }
            }
        }
        return issues;
    }

    private static void count(
            List<Issue> issues, Schema.Index index, String singular, String plural, int count, int[] rule) {
        int min = rule[0];
        int max = rule[1];
        if (count >= min && count <= max) {
            return;
        }
        String expected;
        if (max == 0) {
            expected = "no " + plural;
        } else if (min == max) {
            expected = "exactly " + min + " " + (min == 1 ? singular : plural);
        } else {
            expected = "at least " + min + " " + (min == 1 ? singular : plural);
        }
        Map<String, String> details = new LinkedHashMap<>();
        details.put("type", index.type().value());
        details.put("count", String.valueOf(count));
        issues.add(Issue.of(
                "invalid_" + index.owner() + "_index_" + singular + "_count",
                "A " + index.type().value() + " index must have " + expected + " but '" + index.id() + "' has " + count,
                index.path() + "." + plural,
                details));
    }

    /** The minimum and maximum number of properties of an index type. */
    private static int[] propertyRule(IndexType type) {
        switch (type) {
            case TEXT:
            case POINT:
            case VECTOR:
                return new int[] {1, 1};
            case LOOKUP:
                return new int[] {0, 0};
            default:
                return new int[] {1, Integer.MAX_VALUE};
        }
    }

    /** The minimum and maximum number of labels of a node index type. */
    private static int[] labelRule(IndexType type) {
        switch (type) {
            case FULLTEXT:
                return new int[] {1, Integer.MAX_VALUE};
            case LOOKUP:
                return new int[] {0, 0};
            default:
                return new int[] {1, 1};
        }
    }

    @Override
    public Set<Validator> dependsOn() {
        return Dependencies.core();
    }

    @Override
    public String toString() {
        return "IndexShapeValidator";
    }
}
