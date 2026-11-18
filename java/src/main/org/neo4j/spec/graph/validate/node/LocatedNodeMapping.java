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
// Generated from org/neo4j/spec/graph/validate/node/LocatedNodeMapping.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.Objects;
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping;

public final class LocatedNodeMapping {
    private final String mappingId;

    private final int index;

    private final NodeMapping mapping;

    public LocatedNodeMapping(String mappingId, int index, NodeMapping mapping) {
        this.mappingId = mappingId;
        this.index = index;
        this.mapping = mapping;
    }

    public String getMappingId() {
        return this.mappingId;
    }

    public int getIndex() {
        return this.index;
    }

    public NodeMapping getMapping() {
        return this.mapping;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LocatedNodeMapping)) return false;
        LocatedNodeMapping that = (LocatedNodeMapping) other;
        return Objects.equals(mappingId, that.mappingId)
                && index == that.index
                && Objects.equals(mapping, that.mapping);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(mappingId);
        result = 31 * result + Integer.hashCode(index);
        result = 31 * result + Objects.hashCode(mapping);
        return result;
    }

    @Override
    public String toString() {
        return "LocatedNodeMapping(mappingId=" + mappingId + ", index=" + index + ", mapping=" + mapping + ")";
    }
}
