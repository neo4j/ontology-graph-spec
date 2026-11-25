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
package org.neo4j.spec.graph.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.property.Property;

class NotationImmutableInputTest {

    @Test
    void longhandAndShorthandWorkWithImmutableInputCollections() {
        Node node = Node.builder()
                .label("Person")
                .properties(Map.of("id", Property.builder().key(true).build()))
                .constraints(Map.of())
                .build();
        GraphModel model = GraphModel.builder("1.0", "m")
                .nodes(Map.of("person", node))
                .relationships(Map.of())
                .build();

        model.longhand();
        assertEquals(1, node.getConstraints().size());

        model.shorthand();
        assertTrue(node.getConstraints().isEmpty());
        assertEquals(true, node.getProperties().get("id").getKey());
    }
}
