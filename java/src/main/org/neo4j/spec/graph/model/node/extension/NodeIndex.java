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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;
import org.neo4j.spec.graph.model.index.IndexOption;
import org.neo4j.spec.graph.model.type.IndexType;
import org.neo4j.spec.graph.model.type.Named;

/** An index on a node. */
public final class NodeIndex implements CustomExtensions, Named {
    private IndexType type;

    private final List<String> labels;

    private final List<String> properties;

    private final IndexOption options;

    private final Map<String, Object> custom;

    private String name;

    private NodeIndex(Builder builder) {
        this.type = builder.type;
        this.labels = builder.labels;
        this.properties = builder.properties;
        this.options = builder.options;
        this.custom = builder.custom;
        this.name = builder.name;
    }

    public static Builder builder(IndexType type, List<String> labels, List<String> properties) {
        return new Builder(type, labels, properties);
    }

    /** Type of the index. */
    public IndexType getType() {
        return this.type;
    }

    public void setType(IndexType value) {
        this.type = value;
    }

    /** Labels the index covers. */
    public List<String> getLabels() {
        return this.labels;
    }

    /** Ids of the properties the index covers. */
    public List<String> getProperties() {
        return this.properties;
    }

    /** Configuration specific to the type of index. */
    public IndexOption getOptions() {
        return this.options;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    /** Name of the index in the database. */
    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NodeIndex)) return false;
        NodeIndex that = (NodeIndex) other;
        return Objects.equals(type, that.type)
                && Objects.equals(labels, that.labels)
                && Objects.equals(properties, that.properties)
                && Objects.equals(options, that.options)
                && Objects.equals(custom, that.custom)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, labels, properties, options, custom, name);
    }

    @Override
    public String toString() {
        return "NodeIndex(type=" + type + ", labels=" + labels + ", properties=" + properties + ", options=" + options
                + ", custom=" + custom + ", name=" + name + ")";
    }

    public static final class Builder {
        private IndexType type;
        private List<String> labels;
        private List<String> properties;
        private IndexOption options = null;
        private Map<String, Object> custom = new LinkedHashMap<>();
        private String name = null;

        private Builder(IndexType type, List<String> labels, List<String> properties) {
            this.type = type;
            this.labels = labels;
            this.properties = properties;
        }

        public Builder type(IndexType type) {
            this.type = type;
            return this;
        }

        public Builder labels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder properties(List<String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder options(IndexOption options) {
            this.options = options;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public NodeIndex build() {
            Objects.requireNonNull(type, "Field 'type' is required");
            Objects.requireNonNull(labels, "Field 'labels' is required");
            Objects.requireNonNull(properties, "Field 'properties' is required");
            return new NodeIndex(this);
        }
    }
}
