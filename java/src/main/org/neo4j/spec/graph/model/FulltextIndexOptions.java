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

/** Configuration of a full-text index. */
public final class FulltextIndexOptions extends IndexOptions {
    private final String defaultAnalyzer;
    private final String analyzer;
    private final Boolean eventuallyConsistent;
    private final Integer eventuallyConsistentApplyParallelism;
    private final String eventuallyConsistentRefreshInterval;
    private final Integer eventuallyConsistentRefreshParallelism;
    private final Integer eventuallyConsistentIndexUpdateQueueMaxLength;

    private FulltextIndexOptions(Builder b) {
        this.defaultAnalyzer = b.defaultAnalyzer;
        this.analyzer = b.analyzer;
        this.eventuallyConsistent = b.eventuallyConsistent;
        this.eventuallyConsistentApplyParallelism = b.eventuallyConsistentApplyParallelism;
        this.eventuallyConsistentRefreshInterval = b.eventuallyConsistentRefreshInterval;
        this.eventuallyConsistentRefreshParallelism = b.eventuallyConsistentRefreshParallelism;
        this.eventuallyConsistentIndexUpdateQueueMaxLength = b.eventuallyConsistentIndexUpdateQueueMaxLength;
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

    /** @return analyzer used when the index does not specify one (wire key {@code fulltext.default_analyzer}). */
    public Optional<String> defaultAnalyzer() {
        return Optional.ofNullable(defaultAnalyzer);
    }

    /** @return analyzer used by the index (wire key {@code fulltext.analyzer}). */
    public Optional<String> analyzer() {
        return Optional.ofNullable(analyzer);
    }

    /** @return whether the index is eventually consistent (wire key {@code fulltext.eventually_consistent}). */
    public Optional<Boolean> eventuallyConsistent() {
        return Optional.ofNullable(eventuallyConsistent);
    }

    /** @return threads used to apply updates to an eventually consistent index (wire key {@code fulltext.eventually_consistent_apply_parallelism}). */
    public Optional<Integer> eventuallyConsistentApplyParallelism() {
        return Optional.ofNullable(eventuallyConsistentApplyParallelism);
    }

    /** @return how often an eventually consistent index is refreshed (wire key {@code fulltext.eventually_consistent_refresh_interval}). */
    public Optional<String> eventuallyConsistentRefreshInterval() {
        return Optional.ofNullable(eventuallyConsistentRefreshInterval);
    }

    /** @return threads used to refresh an eventually consistent index (wire key {@code fulltext.eventually_consistent_refresh_parallelism}). */
    public Optional<Integer> eventuallyConsistentRefreshParallelism() {
        return Optional.ofNullable(eventuallyConsistentRefreshParallelism);
    }

    /** @return maximum pending updates queued for an eventually consistent index (wire key {@code fulltext.eventually_consistent_index_update_queue_max_length}). */
    public Optional<Integer> eventuallyConsistentIndexUpdateQueueMaxLength() {
        return Optional.ofNullable(eventuallyConsistentIndexUpdateQueueMaxLength);
    }

    @Override
    public IndexType type() {
        return IndexType.FULLTEXT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FulltextIndexOptions that = (FulltextIndexOptions) o;
        return Objects.equals(defaultAnalyzer, that.defaultAnalyzer)
                && Objects.equals(analyzer, that.analyzer)
                && Objects.equals(eventuallyConsistent, that.eventuallyConsistent)
                && Objects.equals(eventuallyConsistentApplyParallelism, that.eventuallyConsistentApplyParallelism)
                && Objects.equals(eventuallyConsistentRefreshInterval, that.eventuallyConsistentRefreshInterval)
                && Objects.equals(eventuallyConsistentRefreshParallelism, that.eventuallyConsistentRefreshParallelism)
                && Objects.equals(
                        eventuallyConsistentIndexUpdateQueueMaxLength,
                        that.eventuallyConsistentIndexUpdateQueueMaxLength);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                defaultAnalyzer,
                analyzer,
                eventuallyConsistent,
                eventuallyConsistentApplyParallelism,
                eventuallyConsistentRefreshInterval,
                eventuallyConsistentRefreshParallelism,
                eventuallyConsistentIndexUpdateQueueMaxLength);
    }

    @Override
    public String toString() {
        return "FulltextIndexOptions{" + "defaultAnalyzer="
                + defaultAnalyzer + ", analyzer="
                + analyzer + ", eventuallyConsistent="
                + eventuallyConsistent + ", eventuallyConsistentApplyParallelism="
                + eventuallyConsistentApplyParallelism + ", eventuallyConsistentRefreshInterval="
                + eventuallyConsistentRefreshInterval + ", eventuallyConsistentRefreshParallelism="
                + eventuallyConsistentRefreshParallelism + ", eventuallyConsistentIndexUpdateQueueMaxLength="
                + eventuallyConsistentIndexUpdateQueueMaxLength + "}";
    }

    /** Builder for {@link FulltextIndexOptions}. */
    public static final class Builder {
        private String defaultAnalyzer;
        private String analyzer;
        private Boolean eventuallyConsistent;
        private Integer eventuallyConsistentApplyParallelism;
        private String eventuallyConsistentRefreshInterval;
        private Integer eventuallyConsistentRefreshParallelism;
        private Integer eventuallyConsistentIndexUpdateQueueMaxLength;

        private Builder() {}

        private Builder(FulltextIndexOptions v) {
            this.defaultAnalyzer = v.defaultAnalyzer;
            this.analyzer = v.analyzer;
            this.eventuallyConsistent = v.eventuallyConsistent;
            this.eventuallyConsistentApplyParallelism = v.eventuallyConsistentApplyParallelism;
            this.eventuallyConsistentRefreshInterval = v.eventuallyConsistentRefreshInterval;
            this.eventuallyConsistentRefreshParallelism = v.eventuallyConsistentRefreshParallelism;
            this.eventuallyConsistentIndexUpdateQueueMaxLength = v.eventuallyConsistentIndexUpdateQueueMaxLength;
        }

        /**
         * Sets analyzer used when the index does not specify one (wire key {@code fulltext.default_analyzer}).
         *
         * @param defaultAnalyzer the value, or null to clear it
         * @return this builder
         */
        public Builder defaultAnalyzer(String defaultAnalyzer) {
            this.defaultAnalyzer = defaultAnalyzer;
            return this;
        }

        /**
         * Sets analyzer used by the index (wire key {@code fulltext.analyzer}).
         *
         * @param analyzer the value, or null to clear it
         * @return this builder
         */
        public Builder analyzer(String analyzer) {
            this.analyzer = analyzer;
            return this;
        }

        /**
         * Sets whether the index is eventually consistent (wire key {@code fulltext.eventually_consistent}).
         *
         * @param eventuallyConsistent the value, or null to clear it
         * @return this builder
         */
        public Builder eventuallyConsistent(Boolean eventuallyConsistent) {
            this.eventuallyConsistent = eventuallyConsistent;
            return this;
        }

        /**
         * Sets threads used to apply updates to an eventually consistent index (wire key {@code fulltext.eventually_consistent_apply_parallelism}).
         *
         * @param eventuallyConsistentApplyParallelism the value, or null to clear it
         * @return this builder
         */
        public Builder eventuallyConsistentApplyParallelism(Integer eventuallyConsistentApplyParallelism) {
            this.eventuallyConsistentApplyParallelism = eventuallyConsistentApplyParallelism;
            return this;
        }

        /**
         * Sets how often an eventually consistent index is refreshed (wire key {@code fulltext.eventually_consistent_refresh_interval}).
         *
         * @param eventuallyConsistentRefreshInterval the value, or null to clear it
         * @return this builder
         */
        public Builder eventuallyConsistentRefreshInterval(String eventuallyConsistentRefreshInterval) {
            this.eventuallyConsistentRefreshInterval = eventuallyConsistentRefreshInterval;
            return this;
        }

        /**
         * Sets threads used to refresh an eventually consistent index (wire key {@code fulltext.eventually_consistent_refresh_parallelism}).
         *
         * @param eventuallyConsistentRefreshParallelism the value, or null to clear it
         * @return this builder
         */
        public Builder eventuallyConsistentRefreshParallelism(Integer eventuallyConsistentRefreshParallelism) {
            this.eventuallyConsistentRefreshParallelism = eventuallyConsistentRefreshParallelism;
            return this;
        }

        /**
         * Sets maximum pending updates queued for an eventually consistent index (wire key {@code fulltext.eventually_consistent_index_update_queue_max_length}).
         *
         * @param eventuallyConsistentIndexUpdateQueueMaxLength the value, or null to clear it
         * @return this builder
         */
        public Builder eventuallyConsistentIndexUpdateQueueMaxLength(
                Integer eventuallyConsistentIndexUpdateQueueMaxLength) {
            this.eventuallyConsistentIndexUpdateQueueMaxLength = eventuallyConsistentIndexUpdateQueueMaxLength;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public FulltextIndexOptions build() {
            return new FulltextIndexOptions(this);
        }
    }
}
