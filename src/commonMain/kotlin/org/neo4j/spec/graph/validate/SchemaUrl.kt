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
package org.neo4j.spec.graph.validate

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.Version

/**
 * The model's `schema` url must be the url of a released graph spec version. Surrounding whitespace, trailing slashes
 * and a missing `schema.json` are tolerated, as they are by [Version.parseSchemaVersion].
 */
object SchemaUrl : Validation {
    override fun validate(model: GraphModel, issues: MutableList<Issue>) {
        val schema = model.schema
        val base = schema.trim().trimEnd('/').removeSuffix(Version.SCHEMA_FILE).trimEnd('/')
        if (Version.GRAPH_SPEC_VERSIONS.none { "${Version.SCHEMA_BASE}$it" == base }) {
            issues.add(
                Issue(
                    code = "invalid_schema_url",
                    message = "Schema '$schema' is not the url of a released graph spec schema",
                    path = jsonPointerOf("schema")
                )
            )
        }
    }
}
