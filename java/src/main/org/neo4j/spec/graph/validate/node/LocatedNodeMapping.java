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
