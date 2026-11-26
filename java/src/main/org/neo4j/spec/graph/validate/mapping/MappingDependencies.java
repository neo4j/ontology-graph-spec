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
package org.neo4j.spec.graph.validate.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.mapping.Mapping;
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping;
import org.neo4j.spec.graph.model.extension.mapping.QueryMapping;
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;

/**
 * A mapping's `depends_on` must reference the `id` of another mapping in the same list. Ids must be unique within
 * their list and the dependencies free of cycles so an import order can be derived.
 */
public final class MappingDependencies implements Validation {
    public static final MappingDependencies INSTANCE = new MappingDependencies();

    private MappingDependencies() {}

    @Override
    public void validate(GraphModel model, List<Issue> issues) {
        Map<String, List<Mapping>> lists = model.getExtensions().getDataSourceMappings();
        for (var entry : lists.entrySet()) {
            var listId = entry.getKey();
            var mappings = entry.getValue();
            validateList(listId, mappings, lists, issues);
        }
    }

    private void validateList(
            String listId,
            List<? extends Mapping> mappings,
            Map<String, ? extends List<? extends Mapping>> lists,
            List<Issue> issues) {
        Map<String, Integer> indexById = new LinkedHashMap<>();
        for (int index = 0; index < mappings.size(); index++) {
            var mapping = mappings.get(index);
            String id = mappingId(mapping);
            if (id.isEmpty()) {
                continue;
            }
            if (indexById.containsKey(id)) {
                issues.add(new Issue(
                        "mapping_id_duplicate",
                        "Mapping id '" + id + "' is used by more than one mapping in '" + listId + "'",
                        Issue.jsonPointerOf("extensions", "data_source_mappings", listId, index, "id")));
            } else {
                indexById.put(id, index);
            }
        }
        Map<Integer, Integer> next = new LinkedHashMap<>();
        for (int index = 0; index < mappings.size(); index++) {
            var mapping = mappings.get(index);
            String dependsOn = mappingDependsOn(mapping);
            if (dependsOn.isEmpty()) {
                continue;
            }
            Integer target = indexById.get(dependsOn);
            String path = Issue.jsonPointerOf("extensions", "data_source_mappings", listId, index, "depends_on");
            if (Objects.equals(target, index)) {
                issues.add(new Issue(
                        "mapping_depends_on_self", "Mapping '" + dependsOn + "' can't depend on itself", path));
            } else if (target != null) {
                next.put(index, target);
            } else if (isInOtherList(lists, listId, dependsOn)) {
                issues.add(new Issue(
                        "mapping_depends_on_other_list",
                        "Mapping depends on '" + dependsOn + "' which is in a different list of mappings, "
                                + "mappings can only depend on others in the same list",
                        path));
            } else {
                issues.add(new Issue(
                        "mapping_depends_on_unknown",
                        "Mapping depends on '" + dependsOn + "' which isn't the id of any mapping in '" + listId + "'",
                        path));
            }
        }
        reportCycles(listId, next, issues);
    }

    private boolean isInOtherList(
            Map<String, ? extends List<? extends Mapping>> lists, String listId, String mappingId) {
        for (var entry : lists.entrySet()) {
            if (Objects.equals(entry.getKey(), listId)) {
                continue;
            }
            for (var other : entry.getValue()) {
                if (Objects.equals(mappingId(other), mappingId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reportCycles(String listId, Map<Integer, Integer> next, List<Issue> issues) {
        Set<Integer> done = new LinkedHashSet<>();
        for (var start : new TreeSet<>(next.keySet())) {
            if (done.contains(start)) {
                continue;
            }
            List<Integer> path = new ArrayList<>();
            Integer current = start;
            while (current != null && !done.contains(current) && !path.contains(current)) {
                path.add(current);
                current = next.get(current);
            }
            if (current != null && path.contains(current)) {
                List<Integer> cycle = path.subList(path.indexOf(current), path.size());
                List<Integer> sortedCycle = new ArrayList<>(cycle);
                Collections.sort(sortedCycle);
                int first = sortedCycle.get(0);
                issues.add(new Issue(
                        "mapping_dependency_cycle",
                        "Mappings "
                                + sortedCycle.stream().map(it -> "[" + it + "]").collect(Collectors.joining(", "))
                                + " in '" + listId + "' depend on each other in a cycle",
                        Issue.jsonPointerOf("extensions", "data_source_mappings", listId, first, "depends_on")));
            }
            done.addAll(path);
        }
    }

    private String mappingId(Mapping mapping) {
        if (mapping instanceof NodeMapping) {
            return ((NodeMapping) mapping).getId();
        } else if (mapping instanceof RelationshipMapping) {
            return ((RelationshipMapping) mapping).getId();
        } else if (mapping instanceof QueryMapping) {
            return ((QueryMapping) mapping).getId();
        } else {
            throw new IllegalStateException("Unexpected when branch");
        }
    }

    private String mappingDependsOn(Mapping mapping) {
        if (mapping instanceof NodeMapping) {
            return ((NodeMapping) mapping).getDependsOn();
        } else if (mapping instanceof RelationshipMapping) {
            return ((RelationshipMapping) mapping).getDependsOn();
        } else if (mapping instanceof QueryMapping) {
            return ((QueryMapping) mapping).getDependsOn();
        } else {
            throw new IllegalStateException("Unexpected when branch");
        }
    }
}
