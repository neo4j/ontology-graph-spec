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
// Generated from org/neo4j/spec/graph/model/index/FullTextIndexOption.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.index;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/full-text-indexes/#configuration-settings
 * https://neo4j.com/docs/operations-manual/2026.07/performance/index-configuration/#index-configuration-fulltext
 * https://neo4j.com/docs/operations-manual/2026.07/configuration/configuration-settings/#index-settings
 */
@JsonClassDescription("Configuration of a full-text index.")
@JsonTypeName("FULLTEXT")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class FullTextIndexOption implements IndexOption {
    private final String defaultAnalyzer;

    private final String analyzer;

    private final boolean eventuallyConsistent;

    private final int eventuallyConsistentApplyParallelism;

    private final String eventuallyConsistentRefreshInterval;

    private final int eventuallyConsistentRefreshParallelism;

    private final int eventuallyConsistentUpdateQueueMaxLength;

    public FullTextIndexOption(
            String defaultAnalyzer,
            String analyzer,
            boolean eventuallyConsistent,
            int eventuallyConsistentApplyParallelism,
            String eventuallyConsistentRefreshInterval,
            int eventuallyConsistentRefreshParallelism,
            int eventuallyConsistentUpdateQueueMaxLength) {
        this.defaultAnalyzer = defaultAnalyzer;
        this.analyzer = analyzer;
        this.eventuallyConsistent = eventuallyConsistent;
        this.eventuallyConsistentApplyParallelism = eventuallyConsistentApplyParallelism;
        this.eventuallyConsistentRefreshInterval = eventuallyConsistentRefreshInterval;
        this.eventuallyConsistentRefreshParallelism = eventuallyConsistentRefreshParallelism;
        this.eventuallyConsistentUpdateQueueMaxLength = eventuallyConsistentUpdateQueueMaxLength;
    }

    public FullTextIndexOption(
            String defaultAnalyzer,
            String analyzer,
            boolean eventuallyConsistent,
            int eventuallyConsistentApplyParallelism,
            String eventuallyConsistentRefreshInterval,
            int eventuallyConsistentRefreshParallelism) {
        this(
                defaultAnalyzer,
                analyzer,
                eventuallyConsistent,
                eventuallyConsistentApplyParallelism,
                eventuallyConsistentRefreshInterval,
                eventuallyConsistentRefreshParallelism,
                10000);
    }

    public FullTextIndexOption(
            String defaultAnalyzer,
            String analyzer,
            boolean eventuallyConsistent,
            int eventuallyConsistentApplyParallelism,
            String eventuallyConsistentRefreshInterval) {
        this(
                defaultAnalyzer,
                analyzer,
                eventuallyConsistent,
                eventuallyConsistentApplyParallelism,
                eventuallyConsistentRefreshInterval,
                1,
                10000);
    }

    public FullTextIndexOption(
            String defaultAnalyzer,
            String analyzer,
            boolean eventuallyConsistent,
            int eventuallyConsistentApplyParallelism) {
        this(defaultAnalyzer, analyzer, eventuallyConsistent, eventuallyConsistentApplyParallelism, "0s", 1, 10000);
    }

    public FullTextIndexOption(String defaultAnalyzer, String analyzer, boolean eventuallyConsistent) {
        this(defaultAnalyzer, analyzer, eventuallyConsistent, 1, "0s", 1, 10000);
    }

    public FullTextIndexOption(String defaultAnalyzer, String analyzer) {
        this(defaultAnalyzer, analyzer, false, 1, "0s", 1, 10000);
    }

    public FullTextIndexOption(String defaultAnalyzer) {
        this(defaultAnalyzer, null, false, 1, "0s", 1, 10000);
    }

    public FullTextIndexOption() {
        this("standard-no-stop-words", null, false, 1, "0s", 1, 10000);
    }

    public String getDefaultAnalyzer() {
        return this.defaultAnalyzer;
    }

    public String getAnalyzer() {
        return this.analyzer;
    }

    public boolean getEventuallyConsistent() {
        return this.eventuallyConsistent;
    }

    public int getEventuallyConsistentApplyParallelism() {
        return this.eventuallyConsistentApplyParallelism;
    }

    public String getEventuallyConsistentRefreshInterval() {
        return this.eventuallyConsistentRefreshInterval;
    }

    public int getEventuallyConsistentRefreshParallelism() {
        return this.eventuallyConsistentRefreshParallelism;
    }

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
        int result = Objects.hashCode(defaultAnalyzer);
        result = 31 * result + Objects.hashCode(analyzer);
        result = 31 * result + Boolean.hashCode(eventuallyConsistent);
        result = 31 * result + Integer.hashCode(eventuallyConsistentApplyParallelism);
        result = 31 * result + Objects.hashCode(eventuallyConsistentRefreshInterval);
        result = 31 * result + Integer.hashCode(eventuallyConsistentRefreshParallelism);
        result = 31 * result + Integer.hashCode(eventuallyConsistentUpdateQueueMaxLength);
        return result;
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

    @JsonProperty("fulltext.default_analyzer")
    @JsonPropertyDescription("Analyzer used when the index doesn't specify one.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonDefaultAnalyzer() {
        return Objects.equals(this.defaultAnalyzer, "standard-no-stop-words") ? null : this.defaultAnalyzer;
    }

    @JsonProperty("fulltext.analyzer")
    @JsonPropertyDescription("Analyzer used by the index.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonAnalyzer() {
        return Objects.equals(this.analyzer, null) ? null : this.analyzer;
    }

    @JsonProperty("fulltext.eventually_consistent")
    @JsonPropertyDescription(
            "Whether the index is eventually consistent, updating asynchronously after transactions commit.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean jsonEventuallyConsistent() {
        return this.eventuallyConsistent == false ? null : this.eventuallyConsistent;
    }

    @JsonProperty("fulltext.eventually_consistent_apply_parallelism")
    @JsonPropertyDescription("Number of threads used to apply updates to an eventually consistent index.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonEventuallyConsistentApplyParallelism() {
        return this.eventuallyConsistentApplyParallelism == 1 ? null : this.eventuallyConsistentApplyParallelism;
    }

    @JsonProperty("fulltext.eventually_consistent_refresh_interval")
    @JsonPropertyDescription("How often an eventually consistent index is refreshed.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonEventuallyConsistentRefreshInterval() {
        return Objects.equals(this.eventuallyConsistentRefreshInterval, "0s")
                ? null
                : this.eventuallyConsistentRefreshInterval;
    }

    @JsonProperty("fulltext.eventually_consistent_refresh_parallelism")
    @JsonPropertyDescription("Number of threads used to refresh an eventually consistent index.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonEventuallyConsistentRefreshParallelism() {
        return this.eventuallyConsistentRefreshParallelism == 1 ? null : this.eventuallyConsistentRefreshParallelism;
    }

    @JsonProperty("fulltext.eventually_consistent_index_update_queue_max_length")
    @JsonPropertyDescription("Maximum number of pending updates queued for an eventually consistent index.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonEventuallyConsistentUpdateQueueMaxLength() {
        return this.eventuallyConsistentUpdateQueueMaxLength == 10000
                ? null
                : this.eventuallyConsistentUpdateQueueMaxLength;
    }

    @JsonCreator
    private static FullTextIndexOption fromJson(
            @JsonProperty("fulltext.default_analyzer") String defaultAnalyzer,
            @JsonProperty("fulltext.analyzer") String analyzer,
            @JsonProperty("fulltext.eventually_consistent") Boolean eventuallyConsistent,
            @JsonProperty("fulltext.eventually_consistent_apply_parallelism")
                    Integer eventuallyConsistentApplyParallelism,
            @JsonProperty("fulltext.eventually_consistent_refresh_interval") String eventuallyConsistentRefreshInterval,
            @JsonProperty("fulltext.eventually_consistent_refresh_parallelism")
                    Integer eventuallyConsistentRefreshParallelism,
            @JsonProperty("fulltext.eventually_consistent_index_update_queue_max_length")
                    Integer eventuallyConsistentUpdateQueueMaxLength) {
        return new FullTextIndexOption(
                defaultAnalyzer != null ? defaultAnalyzer : "standard-no-stop-words",
                analyzer != null ? analyzer : null,
                eventuallyConsistent != null ? eventuallyConsistent : false,
                eventuallyConsistentApplyParallelism != null ? eventuallyConsistentApplyParallelism : 1,
                eventuallyConsistentRefreshInterval != null ? eventuallyConsistentRefreshInterval : "0s",
                eventuallyConsistentRefreshParallelism != null ? eventuallyConsistentRefreshParallelism : 1,
                eventuallyConsistentUpdateQueueMaxLength != null ? eventuallyConsistentUpdateQueueMaxLength : 10000);
    }
}
