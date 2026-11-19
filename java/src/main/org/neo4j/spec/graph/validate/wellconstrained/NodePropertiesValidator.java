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
package org.neo4j.spec.graph.validate.wellconstrained;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/** Checks every node has properties. Reports {@code node_without_properties} at the node. */
public final class NodePropertiesValidator implements Validator {

    static final NodePropertiesValidator INSTANCE = new NodePropertiesValidator();

    private NodePropertiesValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Node> entry : model.nodes().entrySet()) {
            if (entry.getValue().properties().isEmpty()) {
                issues.add(Issue.of(
                        "node_without_properties",
                        "The node '" + entry.getKey() + "' has no properties",
                        "nodes." + entry.getKey()));
            }
        }
        return issues;
    }

    @Override
    public String toString() {
        return "NodePropertiesValidator";
    }
}
