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
package org.neo4j.spec.graph.model

object Version {
    /*
        Data model versions
     */
    const val DATA_MODEL_V2_3 = "2.3.0"
    const val DATA_MODEL_V2_4 = "2.4.0"
    const val DATA_MODEL_V3_0 = "3.0.0"

    /*
        Wrapped data model versions
     */
    const val DATA_MODEL_WRAPPED_V3_0 = "3.0.0"

    /*
        Import Spec versions
     */
    const val IMPORT_SPEC_V1 = "1.0.0"

    /*
        Graph spec versions
     */
    const val V1 = "1.0.0"
    const val V1_1 = "1.1.0"
    const val LATEST = V1_1

    /**
     * Every released graph spec version, oldest first, ending with [LATEST].
     * Check-list:
     *  - Update [LATEST]
     *  - Migration support in [org.neo4j.spec.graph.defaultConfig]
     *  - Update consts in /go/migration/migration.go
     *  - Update docs
     *    - `README.md`
     *    - `docs/modules/format/pages/getting-started.adoc`
     *    - `docs/modules/format/pages/versioning.adoc`
     *    - `docs/modules/format/pages/reference/model.adoc`
     *    - `docs/modules/libraries/pages/migration.adoc`
     */
    val GRAPH_SPEC_VERSIONS = listOf(V1, LATEST)

    /*
        Schema urls, the spec version is always the path segment before schema.json.
     */
    const val SCHEMA_BASE = "https://neo4j.com/docs/ontology-graph-spec/"

    const val SCHEMA_FILE = "schema.json"

    fun schemaUrl(version: String): String = "$SCHEMA_BASE$version/$SCHEMA_FILE"

    fun parseSchemaVersion(schema: String): String {
        val version = schema.trim().trimEnd('/').removeSuffix(SCHEMA_FILE).trimEnd('/').substringAfterLast('/')
        require(version.isNotBlank()) { "Unable to determine version from schema '$schema'" }
        return version
    }
}
