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
// Generated from org/neo4j/spec/graph/validate/node/NodeMappingKeyType.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.property.Neo4jType;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeMappingKeyType implements NodeValidation {
    public static final NodeMappingKeyType INSTANCE = new NodeMappingKeyType();

    private NodeMappingKeyType() {}

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        for (var item : LocatedNodeMappings.findNodeMappings(model, nodeId)) {
            var mappingId = item.getMappingId();
            var index = item.getIndex();
            var nodeMapping = item.getMapping();
            for (var propertyId : nodeMapping.getKey()) {
                Property property = node.getProperties().get(propertyId);
                if (property == null) {
                    continue;
                }
                if (property.getType() != Neo4jType.STRING && property.getType() != Neo4jType.INTEGER) {
                    issues.add(new Issue(
                            "invalid_node_mapping_key_type",
                            "Mapping key '" + propertyId + "' on node '" + nodeId + "' must be STRING or INTEGER",
                            "extensions.data_source_mappings." + mappingId + "[" + index + "].key." + propertyId));
                }
            }
        }
    }
}
