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
package org.neo4j.spec.graph.model.relationship;

import java.util.Objects;

/** One end of a relationship. */
public final class RelationshipTarget {
    private String node;

    private String label;

    private Integer count;

    private Integer minCount;

    private Integer maxCount;

    private RelationshipTarget(Builder builder) {
        this.node = builder.node;
        this.label = builder.label;
        this.count = builder.count;
        this.minCount = builder.minCount;
        this.maxCount = builder.maxCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Id of the node at this end. */
    public String getNode() {
        return this.node;
    }

    public void setNode(String value) {
        this.node = value;
    }

    /** Label of the node at this end. */
    public String getLabel() {
        return this.label;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    /** Exact number of relationships per node at this end. Null when unconstrained. */
    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer value) {
        this.count = value;
    }

    /** Minimum number of relationships per node at this end. */
    public Integer getMinCount() {
        return this.minCount;
    }

    public void setMinCount(Integer value) {
        this.minCount = value;
    }

    /** Maximum number of relationships per node at this end. */
    public Integer getMaxCount() {
        return this.maxCount;
    }

    public void setMaxCount(Integer value) {
        this.maxCount = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RelationshipTarget)) return false;
        RelationshipTarget that = (RelationshipTarget) other;
        return Objects.equals(node, that.node)
                && Objects.equals(label, that.label)
                && Objects.equals(count, that.count)
                && Objects.equals(minCount, that.minCount)
                && Objects.equals(maxCount, that.maxCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, label, count, minCount, maxCount);
    }

    @Override
    public String toString() {
        return "RelationshipTarget(node=" + node + ", label=" + label + ", count=" + count + ", minCount=" + minCount
                + ", maxCount=" + maxCount + ")";
    }

    public static final class Builder {
        private String node = "";
        private String label = "";
        private Integer count = null;
        private Integer minCount = null;
        private Integer maxCount = null;

        private Builder() {}

        public Builder node(String node) {
            this.node = node;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        public Builder minCount(Integer minCount) {
            this.minCount = minCount;
            return this;
        }

        public Builder maxCount(Integer maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public RelationshipTarget build() {
            return new RelationshipTarget(this);
        }
    }
}
