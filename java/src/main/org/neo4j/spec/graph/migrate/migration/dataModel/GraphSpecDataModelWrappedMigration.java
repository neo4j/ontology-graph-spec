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
package org.neo4j.spec.graph.migrate.migration.dataModel;

import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;

/**
 * Graph Spec 4.0 -> wrapped data_model 3.0
 *
 * Direct so that the shortest migration path keeps the envelope fields the flat data model would drop.
 */
public final class GraphSpecDataModelWrappedMigration extends Migration {
    private final GraphSpecDataModelV3Migration dataModel;

    public GraphSpecDataModelWrappedMigration() {
        super(Type.GRAPH_SPEC, Version.V1_1, Type.DATA_MODEL_WRAPPED, Version.DATA_MODEL_V3_0);
        this.dataModel = new GraphSpecDataModelV3Migration();
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        SchemaMap model = this.dataModel.migrate(schema);
        model.putAll(DataModelUtils.schemaMapOf(
                "visualisation",
                DataModelUtils.nullIfEmpty(this.dataModel.convertVisualisation(schema)),
                "description",
                schema.literalOrNull("description")));
        return DataModelWrapper.wrap(model);
    }
}
