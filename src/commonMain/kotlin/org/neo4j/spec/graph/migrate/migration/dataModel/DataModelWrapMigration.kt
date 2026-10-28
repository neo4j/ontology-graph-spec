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
package org.neo4j.spec.graph.migrate.migration.dataModel

import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.migrate.Migration
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version

/**
 * Flat -> wrapped data_model 3.0
 */
class DataModelWrapMigration :
    Migration(
        fromType = Type.DATA_MODEL,
        from = Version.DATA_MODEL_V3_0,
        toType = Type.DATA_MODEL_WRAPPED,
        to = Version.DATA_MODEL_V3_0
    ) {

    override fun migrate(schema: SchemaMap): SchemaMap = DataModelWrapper.wrap(schema)
}
