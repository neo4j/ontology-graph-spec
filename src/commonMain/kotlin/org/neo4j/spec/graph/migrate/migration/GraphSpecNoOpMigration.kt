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
package org.neo4j.spec.graph.migrate.migration

import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.migrate.Migration
import org.neo4j.spec.graph.model.Type

/**
 * Migration between graph spec versions where the format did not change. Only the version is bumped, which
 * [org.neo4j.spec.graph.migrate.MigrationPath] stamps on the migrated model.
 */
class GraphSpecNoOpMigration(from: String, to: String) :
    Migration(fromType = Type.GRAPH_SPEC, from = from, toType = Type.GRAPH_SPEC, to = to) {

    override fun migrate(schema: SchemaMap): SchemaMap = schema
}
