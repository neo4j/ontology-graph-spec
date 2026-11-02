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
package org.neo4j.spec.graph.model.index

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neo4j.spec.graph.model.type.IndexType
import org.neo4j.spec.graph.schema.Description
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

/**
 * https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/full-text-indexes/#configuration-settings
 * https://neo4j.com/docs/operations-manual/2026.07/performance/index-configuration/#index-configuration-fulltext
 * https://neo4j.com/docs/operations-manual/2026.07/configuration/configuration-settings/#index-settings
 */
@JsExport
@Serializable
@SerialName("FULLTEXT")
@Description("Configuration of a full-text index.")
data class FullTextIndexOption @JvmOverloads constructor(
    @SerialName("fulltext.default_analyzer")
    @Description("Analyzer used when the index doesn't specify one.")
    val defaultAnalyzer: String = "standard-no-stop-words",
    @SerialName("fulltext.analyzer")
    @Description("Analyzer used by the index.")
    val analyzer: String? = null,
    @SerialName("fulltext.eventually_consistent")
    @Description("Whether the index is eventually consistent, updating asynchronously after transactions commit.")
    val eventuallyConsistent: Boolean = false,
    @SerialName("fulltext.eventually_consistent_apply_parallelism")
    @Description("Number of threads used to apply updates to an eventually consistent index.")
    val eventuallyConsistentApplyParallelism: Int = 1,
    @SerialName("fulltext.eventually_consistent_refresh_interval")
    @Description("How often an eventually consistent index is refreshed.")
    val eventuallyConsistentRefreshInterval: String = "0s",
    @SerialName("fulltext.eventually_consistent_refresh_parallelism")
    @Description("Number of threads used to refresh an eventually consistent index.")
    val eventuallyConsistentRefreshParallelism: Int = 1,
    @SerialName("fulltext.eventually_consistent_index_update_queue_max_length")
    @Description("Maximum number of pending updates queued for an eventually consistent index.")
    val eventuallyConsistentUpdateQueueMaxLength: Int = 10_000
) : IndexOption {
    override val type: IndexType get() = IndexType.FULLTEXT
}
