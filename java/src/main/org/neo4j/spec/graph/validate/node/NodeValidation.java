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
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;

public interface NodeValidation extends Validation {
    @Override
    default void validate(GraphModel model, List<Issue> issues) {
        for (var entry : model.getNodes().entrySet()) {
            var nodeId = entry.getKey();
            var node = entry.getValue();
            validateNode(model, nodeId, node, issues);
            for (var entry2 : node.getProperties().entrySet()) {
                var propertyId = entry2.getKey();
                var property = entry2.getValue();
                validateProperty(model, nodeId, node, propertyId, property, issues);
            }
            for (var entry2 : node.getConstraints().entrySet()) {
                var constraintId = entry2.getKey();
                var constraint = entry2.getValue();
                validateConstraint(model, nodeId, node, constraintId, constraint, issues);
            }
            for (var entry2 : node.getExtensions().getIndexes().entrySet()) {
                var indexId = entry2.getKey();
                var index = entry2.getValue();
                validateIndex(model, nodeId, node, indexId, index, issues);
            }
        }
    }

    default void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {}

    default void validateProperty(
            GraphModel model, String nodeId, Node node, String propertyId, Property property, List<Issue> issues) {}

    default void validateConstraint(
            GraphModel model,
            String nodeId,
            Node node,
            String constraintId,
            NodeConstraint constraint,
            List<Issue> issues) {}

    default void validateIndex(
            GraphModel model, String nodeId, Node node, String indexId, NodeIndex index, List<Issue> issues) {}
}
