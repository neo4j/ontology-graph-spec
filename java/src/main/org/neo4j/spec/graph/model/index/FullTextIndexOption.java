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
package org.neo4j.spec.graph.model.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/full-text-indexes/#configuration-settings
 * https://neo4j.com/docs/operations-manual/2026.07/performance/index-configuration/#index-configuration-fulltext
 * https://neo4j.com/docs/operations-manual/2026.07/configuration/configuration-settings/#index-settings
 */
/** Configuration of a full-text index. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({
    "fulltext.default_analyzer",
    "fulltext.analyzer",
    "fulltext.eventually_consistent",
    "fulltext.eventually_consistent_apply_parallelism",
    "fulltext.eventually_consistent_refresh_interval",
    "fulltext.eventually_consistent_refresh_parallelism",
    "fulltext.eventually_consistent_index_update_queue_max_length"
})
@JsonDeserialize(builder = FullTextIndexOption.Builder.class)
@JsonTypeName("FULLTEXT")
public final class FullTextIndexOption implements IndexOption {
    private final String defaultAnalyzer;

    private final String analyzer;

    private final boolean eventuallyConsistent;

    private final int eventuallyConsistentApplyParallelism;

    private final String eventuallyConsistentRefreshInterval;

    private final int eventuallyConsistentRefreshParallelism;

    private final int eventuallyConsistentUpdateQueueMaxLength;

    /** Default instance Jackson compares against to omit default values. */
    private FullTextIndexOption() {
        this(new Builder());
    }

    private FullTextIndexOption(Builder builder) {
        this.defaultAnalyzer = builder.defaultAnalyzer;
        this.analyzer = builder.analyzer;
        this.eventuallyConsistent = builder.eventuallyConsistent;
        this.eventuallyConsistentApplyParallelism = builder.eventuallyConsistentApplyParallelism;
        this.eventuallyConsistentRefreshInterval = builder.eventuallyConsistentRefreshInterval;
        this.eventuallyConsistentRefreshParallelism = builder.eventuallyConsistentRefreshParallelism;
        this.eventuallyConsistentUpdateQueueMaxLength = builder.eventuallyConsistentUpdateQueueMaxLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Analyzer used when the index doesn't specify one. */
    @JsonProperty("fulltext.default_analyzer")
    public String getDefaultAnalyzer() {
        return this.defaultAnalyzer;
    }

    /** Analyzer used by the index. */
    @JsonProperty("fulltext.analyzer")
    public String getAnalyzer() {
        return this.analyzer;
    }

    /** Whether the index is eventually consistent, updating asynchronously after transactions commit. */
    @JsonProperty("fulltext.eventually_consistent")
    public boolean getEventuallyConsistent() {
        return this.eventuallyConsistent;
    }

    /** Number of threads used to apply updates to an eventually consistent index. */
    @JsonProperty("fulltext.eventually_consistent_apply_parallelism")
    public int getEventuallyConsistentApplyParallelism() {
        return this.eventuallyConsistentApplyParallelism;
    }

    /** How often an eventually consistent index is refreshed. */
    @JsonProperty("fulltext.eventually_consistent_refresh_interval")
    public String getEventuallyConsistentRefreshInterval() {
        return this.eventuallyConsistentRefreshInterval;
    }

    /** Number of threads used to refresh an eventually consistent index. */
    @JsonProperty("fulltext.eventually_consistent_refresh_parallelism")
    public int getEventuallyConsistentRefreshParallelism() {
        return this.eventuallyConsistentRefreshParallelism;
    }

    /** Maximum number of pending updates queued for an eventually consistent index. */
    @JsonProperty("fulltext.eventually_consistent_index_update_queue_max_length")
    public int getEventuallyConsistentUpdateQueueMaxLength() {
        return this.eventuallyConsistentUpdateQueueMaxLength;
    }

    @Override
    public IndexType getType() {
        return IndexType.FULLTEXT;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FullTextIndexOption)) return false;
        FullTextIndexOption that = (FullTextIndexOption) other;
        return Objects.equals(defaultAnalyzer, that.defaultAnalyzer)
                && Objects.equals(analyzer, that.analyzer)
                && eventuallyConsistent == that.eventuallyConsistent
                && eventuallyConsistentApplyParallelism == that.eventuallyConsistentApplyParallelism
                && Objects.equals(eventuallyConsistentRefreshInterval, that.eventuallyConsistentRefreshInterval)
                && eventuallyConsistentRefreshParallelism == that.eventuallyConsistentRefreshParallelism
                && eventuallyConsistentUpdateQueueMaxLength == that.eventuallyConsistentUpdateQueueMaxLength;
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
                eventuallyConsistentUpdateQueueMaxLength);
    }

    @Override
    public String toString() {
        return "FullTextIndexOption(defaultAnalyzer=" + defaultAnalyzer + ", analyzer=" + analyzer
                + ", eventuallyConsistent=" + eventuallyConsistent + ", eventuallyConsistentApplyParallelism="
                + eventuallyConsistentApplyParallelism + ", eventuallyConsistentRefreshInterval="
                + eventuallyConsistentRefreshInterval + ", eventuallyConsistentRefreshParallelism="
                + eventuallyConsistentRefreshParallelism + ", eventuallyConsistentUpdateQueueMaxLength="
                + eventuallyConsistentUpdateQueueMaxLength + ")";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String defaultAnalyzer = "standard-no-stop-words";
        private String analyzer = null;
        private boolean eventuallyConsistent = false;
        private int eventuallyConsistentApplyParallelism = 1;
        private String eventuallyConsistentRefreshInterval = "0s";
        private int eventuallyConsistentRefreshParallelism = 1;
        private int eventuallyConsistentUpdateQueueMaxLength = 10000;

        private Builder() {}

        @JsonProperty("fulltext.default_analyzer")
        public Builder defaultAnalyzer(String defaultAnalyzer) {
            this.defaultAnalyzer = defaultAnalyzer;
            return this;
        }

        @JsonProperty("fulltext.analyzer")
        public Builder analyzer(String analyzer) {
            this.analyzer = analyzer;
            return this;
        }

        @JsonProperty("fulltext.eventually_consistent")
        public Builder eventuallyConsistent(boolean eventuallyConsistent) {
            this.eventuallyConsistent = eventuallyConsistent;
            return this;
        }

        @JsonProperty("fulltext.eventually_consistent_apply_parallelism")
        public Builder eventuallyConsistentApplyParallelism(int eventuallyConsistentApplyParallelism) {
            this.eventuallyConsistentApplyParallelism = eventuallyConsistentApplyParallelism;
            return this;
        }

        @JsonProperty("fulltext.eventually_consistent_refresh_interval")
        public Builder eventuallyConsistentRefreshInterval(String eventuallyConsistentRefreshInterval) {
            this.eventuallyConsistentRefreshInterval = eventuallyConsistentRefreshInterval;
            return this;
        }

        @JsonProperty("fulltext.eventually_consistent_refresh_parallelism")
        public Builder eventuallyConsistentRefreshParallelism(int eventuallyConsistentRefreshParallelism) {
            this.eventuallyConsistentRefreshParallelism = eventuallyConsistentRefreshParallelism;
            return this;
        }

        @JsonProperty("fulltext.eventually_consistent_index_update_queue_max_length")
        public Builder eventuallyConsistentUpdateQueueMaxLength(int eventuallyConsistentUpdateQueueMaxLength) {
            this.eventuallyConsistentUpdateQueueMaxLength = eventuallyConsistentUpdateQueueMaxLength;
            return this;
        }

        public FullTextIndexOption build() {
            return new FullTextIndexOption(this);
        }
    }
}
