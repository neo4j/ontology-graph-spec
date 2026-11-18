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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping;

public final class LocatedNodeMappings {
    private LocatedNodeMappings() {}

    /**
     * Finds every mapping of {@code nodeId} along with where each lives in the model's mappings.
     */
    public static List<LocatedNodeMapping> findNodeMappings(GraphModel model, String nodeId) {
        List<LocatedNodeMapping> found = new ArrayList<LocatedNodeMapping>();
        for (var entry : model.getExtensions().getDataSourceMappings().entrySet()) {
            var mappingId = entry.getKey();
            var mappings = entry.getValue();
            for (int index = 0; index < mappings.size(); index++) {
                var mapping = mappings.get(index);
                if (mapping instanceof NodeMapping
                        && Objects.equals(((NodeMapping) ((NodeMapping) mapping)).getNode(), nodeId)) {
                    found.add(new LocatedNodeMapping(mappingId, index, ((NodeMapping) mapping)));
                }
            }
        }
        return found;
    }
}
