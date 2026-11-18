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
// Generated from org/neo4j/spec/graph/GraphSpec.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph;

import java.util.Objects;
import org.neo4j.spec.graph.codec.format.JsonFormat;
import org.neo4j.spec.graph.codec.format.YamlFormat;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.migrate.MigrationPath;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;

public abstract class GraphSpec {
    private final GraphSpecConfig configuration;

    private final MigrationPath path;

    protected GraphSpec(GraphSpecConfig configuration) {
        this.configuration = configuration;
        this.path = new MigrationPath(getConfiguration().getMigrations());
    }

    public GraphSpecConfig getConfiguration() {
        return this.configuration;
    }

    public String encodeToString(GraphModel model, String targetType, String targetVersion) {
        if (!Objects.equals(targetType, Type.GRAPH_SPEC)) {
            model.longhand();
        }
        if (!this.path.requiresMigration(GraphSpecs.specVersion(model), Type.GRAPH_SPEC, targetVersion, targetType)) {
            return getConfiguration().getFormat().encodeModelToString(model);
        }
        SchemaElement schema = getConfiguration().getFormat().encodeToSchema(model);
        SchemaMap map = schema instanceof SchemaMap ? (SchemaMap) schema : null;
        if (map == null) {
            throw new IllegalStateException("Schema format expected");
        }
        map = this.path.migrate(map, Type.GRAPH_SPEC, targetVersion, targetType);
        return getConfiguration().getFormat().encodeToString(map);
    }

    public String encodeToString(GraphModel model, String targetType) {
        return encodeToString(model, targetType, Version.LATEST);
    }

    public String encodeToString(GraphModel model) {
        return encodeToString(model, Type.GRAPH_SPEC, Version.LATEST);
    }

    public GraphModel decodeFromString(String content, String type) {
        GraphModel graphModel = decodeModel(content, type);
        graphModel.longhand();
        return graphModel;
    }

    public GraphModel decodeFromString(String content) {
        return decodeFromString(content, Type.GRAPH_SPEC);
    }

    private GraphModel decodeModel(String content, String type) {
        if (Objects.equals(type, Type.GRAPH_SPEC)) {
            GraphModel model = getConfiguration().getFormat().decodeModelFromString(content);
            if (!this.path.requiresMigration(GraphSpecs.specVersion(model), type, Version.LATEST, Type.GRAPH_SPEC)) {
                return model;
            }
        }
        SchemaElement schema = getConfiguration().getFormat().decodeFromString(content);
        SchemaMap map = schema instanceof SchemaMap ? (SchemaMap) schema : null;
        if (map == null) {
            throw new IllegalStateException("Schema format expected");
        }
        map = this.path.migrate(map, type, Version.LATEST, Type.GRAPH_SPEC);
        return getConfiguration().getFormat().decodeFromSchema(map);
    }

    public static GraphSpec getJson() {
        return Json.INSTANCE;
    }

    public static GraphSpec getYaml() {
        return Yaml.INSTANCE;
    }

    public static final class Json extends GraphSpec {
        public static final Json INSTANCE = new Json();

        private Json() {
            super(GraphSpecs.defaultConfig(JsonFormat.getDefault()));
        }
    }

    public static final class Yaml extends GraphSpec {
        public static final Yaml INSTANCE = new Yaml();

        private Yaml() {
            super(GraphSpecs.defaultConfig(YamlFormat.getDefault()));
        }
    }
}
