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
package org.neo4j.spec.graph.model.node.extension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

/** Extensions for a node. */
public final class NodeExtensions implements CustomExtensions {
    private final Map<String, NodeIndex> indexes;

    private final NodeDisplay display;

    private final Map<String, Object> custom;

    private NodeExtensions(Builder builder) {
        this.indexes = builder.indexes;
        this.display = builder.display;
        this.custom = builder.custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Database indexes on this node keyed by id. */
    public Map<String, NodeIndex> getIndexes() {
        return this.indexes;
    }

    /** Optional visual information of this node. */
    public NodeDisplay getDisplay() {
        return this.display;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NodeExtensions)) return false;
        NodeExtensions that = (NodeExtensions) other;
        return Objects.equals(indexes, that.indexes)
                && Objects.equals(display, that.display)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes, display, custom);
    }

    @Override
    public String toString() {
        return "NodeExtensions(indexes=" + indexes + ", display=" + display + ", custom=" + custom + ")";
    }

    public static final class Builder {
        private Map<String, NodeIndex> indexes = new LinkedHashMap<>();
        private NodeDisplay display = null;
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        public Builder indexes(Map<String, NodeIndex> indexes) {
            this.indexes = indexes;
            return this;
        }

        public Builder display(NodeDisplay display) {
            this.display = display;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public NodeExtensions build() {
            return new NodeExtensions(this);
        }
    }
}
