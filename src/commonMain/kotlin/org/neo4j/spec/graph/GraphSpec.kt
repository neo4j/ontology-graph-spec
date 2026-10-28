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
package org.neo4j.spec.graph

import org.neo4j.spec.graph.codec.format.Format
import org.neo4j.spec.graph.codec.format.JsonFormat
import org.neo4j.spec.graph.codec.format.YamlFormat
import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.migrate.MigrationPath
import org.neo4j.spec.graph.migrate.migration.GraphSpecNoOpMigration
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelUnwrapMigration
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelV2V3Migration
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelV3GraphSpecMigration
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelWrapMigration
import org.neo4j.spec.graph.migrate.migration.dataModel.GraphSpecDataModelV3Migration
import org.neo4j.spec.graph.migrate.migration.dataModel.GraphSpecDataModelWrappedMigration
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import kotlin.js.JsExport

@JsExport
sealed class GraphSpec(val configuration: GraphSpecConfig) {
    private val path = MigrationPath(configuration.migrations)

    fun encodeToString(
        model: GraphModel,
        targetType: String = Type.GRAPH_SPEC,
        targetVersion: String = Version.LATEST
    ): String {
        if (targetType != Type.GRAPH_SPEC) { // Migrations expect longhand graph spec models.
            model.longhand()
        }
        if (!path.requiresMigration(model.specVersion(), Type.GRAPH_SPEC, targetVersion, targetType)) {
            return configuration.format.encodeModelToString(model)
        }
        val schema = configuration.format.encodeToSchema(model)
        var map = schema as? SchemaMap ?: error("Schema format expected")
        map = path.migrate(map, Type.GRAPH_SPEC, targetVersion, targetType)
        return configuration.format.encodeToString(map)
    }

    fun decodeFromString(content: String, type: String = Type.GRAPH_SPEC): GraphModel {
        val graphModel = decodeModel(content, type)
        graphModel.longhand()
        return graphModel
    }

    private fun decodeModel(content: String, type: String): GraphModel {
        if (type == Type.GRAPH_SPEC) {
            val model = configuration.format.decodeModelFromString(content)
            if (!path.requiresMigration(model.specVersion(), type, Version.LATEST, Type.GRAPH_SPEC)) {
                return model
            }
        }
        val schema = configuration.format.decodeFromString(content)
        var map = schema as? SchemaMap ?: error("Schema format expected")
        map = path.migrate(map, type, Version.LATEST, Type.GRAPH_SPEC)
        return configuration.format.decodeFromSchema(map)
    }

    object Json : GraphSpec(defaultConfig(JsonFormat.default))

    object Yaml : GraphSpec(defaultConfig(YamlFormat.default))
}

fun defaultConfig(format: Format): GraphSpecConfig {
    val builder = GraphSpecConfig.Builder(format)
    builder.migrate(DataModelV2V3Migration(Version.DATA_MODEL_V23))
    builder.migrate(DataModelV2V3Migration(Version.DATA_MODEL_V24))
    builder.migrate(DataModelV3GraphSpecMigration())
    builder.migrate(GraphSpecDataModelV3Migration())
    builder.migrate(GraphSpecDataModelWrappedMigration())
    builder.migrate(DataModelUnwrapMigration(Version.DATA_MODEL_V23))
    builder.migrate(DataModelUnwrapMigration(Version.DATA_MODEL_V24))
    builder.migrate(DataModelUnwrapMigration(Version.DATA_MODEL_V30))
    builder.migrate(DataModelWrapMigration())
    builder.migrate(GraphSpecNoOpMigration(Version.V1, Version.V1_1))
    return builder.build()
}

private fun GraphModel.specVersion() = Version.parseSchemaVersion(schema)

private class GraphSpecImpl(configuration: GraphSpecConfig) : GraphSpec(configuration)

fun GraphSpec(from: GraphSpec = GraphSpec.Json, builderAction: GraphSpecConfig.Builder.() -> Unit): GraphSpec {
    val builder = GraphSpecConfig.Builder(from.configuration)
    builder.builderAction()
    val conf = builder.build()
    return GraphSpecImpl(conf)
}
