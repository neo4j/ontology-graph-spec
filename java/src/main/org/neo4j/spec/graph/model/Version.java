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
package org.neo4j.spec.graph.model;

import java.util.List;

public final class Version {
    public static final String DATA_MODEL_V2_3 = "2.3.0";

    public static final String DATA_MODEL_V2_4 = "2.4.0";

    public static final String DATA_MODEL_V3_0 = "3.0.0";

    public static final String DATA_MODEL_WRAPPED_V3_0 = "3.0.0";

    public static final String IMPORT_SPEC_V1 = "1.0.0";

    public static final String V1 = "1.0.0";

    public static final String V1_1 = "1.1.0";

    public static final String LATEST = Version.V1_1;

    private static final List<String> GRAPH_SPEC_VERSIONS = List.of(Version.V1, Version.LATEST);

    public static final String SCHEMA_BASE = "https://neo4j.com/docs/ontology-graph-spec/";

    public static final String SCHEMA_FILE = "schema.json";

    private Version() {}

    /**
     * Every released graph spec version, oldest first, ending with {@code LATEST}.
     * Check-list:
     * - Update {@code LATEST}
     * - Migration support in {@code org.neo4j.spec.graph.defaultConfig}
     * - Update consts in /go/migration/migration.go
     * - Update docs
     * - `README.md`
     * - `docs/modules/format/pages/getting-started.adoc`
     * - `docs/modules/format/pages/versioning.adoc`
     * - `docs/modules/format/pages/reference/model.adoc`
     * - `docs/modules/libraries/pages/migration.adoc`
     */
    public static List<String> GRAPH_SPEC_VERSIONS() {
        return Version.GRAPH_SPEC_VERSIONS;
    }

    public static String schemaUrl(String version) {
        return Version.SCHEMA_BASE + version + "/" + Version.SCHEMA_FILE;
    }

    public static String parseSchemaVersion(String schema) {
        String path = trimTrailingSlashes(schema.strip());
        if (path.endsWith(Version.SCHEMA_FILE)) {
            path = path.substring(0, path.length() - Version.SCHEMA_FILE.length());
        }
        String version = trimTrailingSlashes(path);
        version = version.substring(version.lastIndexOf('/') + 1);
        if (version.isBlank()) {
            throw new IllegalArgumentException("Unable to determine version from schema '" + schema + "'");
        }
        return version;
    }

    private static String trimTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
