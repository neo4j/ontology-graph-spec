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
package org.neo4j.spec.graph.migrate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.migrate.migration.dataModel.DataModelWrapper;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;

public final class MigrationPath {
    private final Map<String, List<Migration>> migrations;

    public MigrationPath(Map<String, List<Migration>> migrations) {
        this.migrations = migrations;
    }

    public Map<String, List<Migration>> getMigrations() {
        return this.migrations;
    }

    public SchemaMap migrate(SchemaMap schema, String type, String targetVersion, String targetType) {
        String from = version(schema, sourceType(schema, type));
        String to = version(targetVersion, targetType);
        List<Migration> path = findPath(from, to);
        if (path == null) {
            throw new IllegalStateException("Unsupported migration from " + from + " to " + to);
        }
        SchemaMap map = schema;
        for (var migration : path) {
            map = migration.migrate(map);
            stampVersion(map, migration);
        }
        return map;
    }

    public boolean requiresMigration(String version, String type, String targetVersion, String targetType) {
        return !Objects.equals(version(version, type), version(targetVersion, targetType));
    }

    private String version(String version, String type) {
        String semver = substringBefore(substringBefore(version, '-'), '+');
        if (semver.chars().filter(c -> c == '.').count() > 1) {
            semver = semver.substring(0, semver.lastIndexOf('.')) + ".0";
        }
        return type + ":" + semver;
    }

    private static String substringBefore(String string, char delimiter) {
        int index = string.indexOf(delimiter);
        return index == -1 ? string : string.substring(0, index);
    }

    /**
     * Wrapped data models are also accepted as plain data models.
     */
    private String sourceType(SchemaMap schema, String type) {
        if (Objects.equals(type, Type.DATA_MODEL) && DataModelWrapper.isWrapped(schema)) {
            return Type.DATA_MODEL_WRAPPED;
        } else {
            return type;
        }
    }

    /**
     * Graph specs identify their spec version via the `schema` url, wrapped data models by their inner data model,
     * whereas the legacy types use `version`.
     */
    private String version(SchemaMap schema, String type) {
        if (Objects.equals(type, Type.GRAPH_SPEC)) {
            String url = schema.stringOrNull("schema");
            if (url == null) {
                throw new IllegalStateException("Schema must be specified");
            }
            return version(Version.parseSchemaVersion(url), type);
        }
        SchemaMap versioned =
                Objects.equals(type, Type.DATA_MODEL_WRAPPED) ? schema.map(DataModelWrapper.DATA_MODEL) : schema;
        String version = versioned.stringOrNull("version");
        if (version == null) {
            throw new IllegalStateException("Version must be specified");
        }
        return version(version, type);
    }

    private void stampVersion(SchemaMap map, Migration migration) {
        String subject = migration.getToType();
        if (Objects.equals(subject, Type.GRAPH_SPEC)) {
            map.set("schema", Version.schemaUrl(migration.getTo()));
        } else if (Objects.equals(subject, Type.DATA_MODEL_WRAPPED)) {
            map.map(DataModelWrapper.DATA_MODEL).set("version", migration.getTo());
        } else {
            map.set("version", migration.getTo());
        }
    }

    /**
     * Basic breath first search to find a migration path between {@code to} and {@code from} versions
     */
    public List<Migration> findPath(String from, String to) {
        if (Objects.equals(from, to)) {
            return Collections.emptyList();
        }
        Map<String, Migration> frontier = new LinkedHashMap<String, Migration>();
        Set<String> visited = new LinkedHashSet<>();
        visited.add(from);
        List<String> stack = new ArrayList<>();
        stack.add(from);
        while (!stack.isEmpty()) {
            String version = stack.remove(0);
            List<Migration> value = getMigrations().get(version);
            if (value == null) {
                continue;
            }
            for (var migration : value) {
                if (visited.contains(migration.getToKey())) {
                    continue;
                }
                visited.add(migration.getToKey());
                frontier.put(migration.getToKey(), migration);
                if (Objects.equals(migration.getToKey(), to)) {
                    return path(from, to, frontier);
                }
                stack.add(migration.getToKey());
            }
        }
        return null;
    }

    /**
     * Backtrace the path
     */
    private List<Migration> path(String start, String end, Map<String, Migration> frontier) {
        List<Migration> reversed = new ArrayList<Migration>();
        String cur = end;
        while (!Objects.equals(cur, start)) {
            Migration m = frontier.get(cur);
            if (m == null) {
                return null;
            }
            reversed.add(0, m);
            cur = m.getFromKey();
        }
        return reversed;
    }
}
