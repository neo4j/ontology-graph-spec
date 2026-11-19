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

import java.util.Objects;
import java.util.Optional;

/** One end of a relationship. */
public final class RelationshipTarget {
    private final String node;
    private final String label;
    private final Integer count;
    private final Integer minCount;
    private final Integer maxCount;

    private RelationshipTarget(Builder b) {
        this.node = b.node;
        this.label = b.label;
        this.count = b.count;
        this.minCount = b.minCount;
        this.maxCount = b.maxCount;
    }

    /**
     * Creates an empty builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialised with the values of this instance.
     *
     * @return a new builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** @return id of the node at this end. */
    public Optional<String> node() {
        return Optional.ofNullable(node);
    }

    /** @return label of the node at this end. */
    public Optional<String> label() {
        return Optional.ofNullable(label);
    }

    /** @return exact number of relationships per node at this end. */
    public Optional<Integer> count() {
        return Optional.ofNullable(count);
    }

    /** @return minimum number of relationships per node at this end (wire key {@code min_count}). */
    public Optional<Integer> minCount() {
        return Optional.ofNullable(minCount);
    }

    /** @return maximum number of relationships per node at this end (wire key {@code max_count}). */
    public Optional<Integer> maxCount() {
        return Optional.ofNullable(maxCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RelationshipTarget that = (RelationshipTarget) o;
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
        return "RelationshipTarget{" + "node="
                + node + ", label="
                + label + ", count="
                + count + ", minCount="
                + minCount + ", maxCount="
                + maxCount + "}";
    }

    /** Builder for {@link RelationshipTarget}. */
    public static final class Builder {
        private String node;
        private String label;
        private Integer count;
        private Integer minCount;
        private Integer maxCount;

        private Builder() {}

        private Builder(RelationshipTarget v) {
            this.node = v.node;
            this.label = v.label;
            this.count = v.count;
            this.minCount = v.minCount;
            this.maxCount = v.maxCount;
        }

        /**
         * Sets id of the node at this end.
         *
         * @param node the value, or null to clear it
         * @return this builder
         */
        public Builder node(String node) {
            this.node = node;
            return this;
        }

        /**
         * Sets label of the node at this end.
         *
         * @param label the value, or null to clear it
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets exact number of relationships per node at this end.
         *
         * @param count the value, or null to clear it
         * @return this builder
         */
        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        /**
         * Sets minimum number of relationships per node at this end (wire key {@code min_count}).
         *
         * @param minCount the value, or null to clear it
         * @return this builder
         */
        public Builder minCount(Integer minCount) {
            this.minCount = minCount;
            return this;
        }

        /**
         * Sets maximum number of relationships per node at this end (wire key {@code max_count}).
         *
         * @param maxCount the value, or null to clear it
         * @return this builder
         */
        public Builder maxCount(Integer maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public RelationshipTarget build() {
            return new RelationshipTarget(this);
        }
    }
}
