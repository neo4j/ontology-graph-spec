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
package org.neo4j.spec.graph;

import java.util.function.Consumer;
import org.neo4j.spec.graph.codec.format.Format;
import org.neo4j.spec.graph.migrate.migration.GraphSpecNoOpMigration;
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelUnwrapMigration;
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelV2V3Migration;
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelV3GraphSpecMigration;
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelWrapMigration;
import org.neo4j.spec.graph.migrate.migration.dataModel.GraphSpecDataModelV3Migration;
import org.neo4j.spec.graph.migrate.migration.dataModel.GraphSpecDataModelWrappedMigration;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Version;

public final class GraphSpecs {
    private GraphSpecs() {}

    public static GraphSpecConfig defaultConfig(Format format) {
        GraphSpecConfig.Builder builder = new GraphSpecConfig.Builder(format);
        builder.migrate(new DataModelV2V3Migration(Version.DATA_MODEL_V2_3));
        builder.migrate(new DataModelV2V3Migration(Version.DATA_MODEL_V2_4));
        builder.migrate(new DataModelV3GraphSpecMigration());
        builder.migrate(new GraphSpecDataModelV3Migration());
        builder.migrate(new GraphSpecDataModelWrappedMigration());
        builder.migrate(new DataModelUnwrapMigration(Version.DATA_MODEL_V2_3));
        builder.migrate(new DataModelUnwrapMigration(Version.DATA_MODEL_V2_4));
        builder.migrate(new DataModelUnwrapMigration(Version.DATA_MODEL_V3_0));
        builder.migrate(new DataModelWrapMigration());
        builder.migrate(new GraphSpecNoOpMigration(Version.V1, Version.V1_1));
        return builder.build();
    }

    static String specVersion(GraphModel graphModel) {
        return Version.parseSchemaVersion(graphModel.getSchema());
    }

    public static GraphSpec GraphSpec(GraphSpec from, Consumer<GraphSpecConfig.Builder> builderAction) {
        GraphSpecConfig.Builder builder = new GraphSpecConfig.Builder(from.getConfiguration());
        builderAction.accept(builder);
        GraphSpecConfig conf = builder.build();
        return new GraphSpecImpl(conf);
    }

    public static GraphSpec GraphSpec(Consumer<GraphSpecConfig.Builder> builderAction) {
        return GraphSpecs.GraphSpec(GraphSpec.Json.INSTANCE, builderAction);
    }
}
