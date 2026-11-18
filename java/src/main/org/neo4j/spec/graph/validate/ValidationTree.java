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
package org.neo4j.spec.graph.validate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.model.GraphModel;

public final class ValidationTree {
    private final Map<Validation, List<Validation>> tree;

    private final List<Validation> roots;

    public ValidationTree() {
        this.tree = new LinkedHashMap<Validation, List<Validation>>();
        this.roots = new ArrayList<Validation>();
    }

    /**
     * Build a tree of validation dependencies using {@code Validation.dependsOn}
     */
    public void build(List<? extends Validation> validators) {
        clear();
        for (var validation : validators) {
            List<Validation> dependencies = validation.dependsOn();
            if (dependencies.isEmpty()) {
                this.roots.add(validation);
                continue;
            }
            for (var dependent : dependencies) {
                this.tree.computeIfAbsent(dependent, key -> new ArrayList<>()).add(validation);
            }
        }
    }

    /**
     * Check a {@code GraphModel} for validation {@code Issue}s.
     */
    public List<Issue> validate(GraphModel model) {
        if (!!this.roots.isEmpty()) {
            throw new IllegalArgumentException(
                    String.valueOf("Validation tree must be built before validating a GraphModel"));
        }
        List<Issue> issues = new ArrayList<Issue>();
        for (var validation : this.roots) {
            validate(validation, model, issues);
        }
        return issues;
    }

    /**
     * Check {@code model} against {@code validation} skipping child validators if an issue was found
     * to avoid duplicate or irrelevant issues.
     */
    private void validate(Validation validation, GraphModel model, List<Issue> issues) {
        int start = issues.size();
        validation.validate(model, issues);
        if (issues.size() > start) {
            return;
        }
        List<Validation> value = this.tree.get(validation);
        if (value == null) {
            return;
        }
        for (var dependency : value) {
            validate(dependency, model, issues);
        }
    }

    public void clear() {
        this.tree.clear();
        this.roots.clear();
    }
}
