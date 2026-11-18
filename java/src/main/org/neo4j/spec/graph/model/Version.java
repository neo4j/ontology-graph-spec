// Generated from org/neo4j/spec/graph/model/Version.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;

public final class Version {
    public static final String DATA_MODEL_V2_3 = "2.3.0";

    public static final String DATA_MODEL_V2_4 = "2.4.0";

    public static final String DATA_MODEL_V3_0 = "3.0.0";

    public static final String DATA_MODEL_WRAPPED_V3_0 = "3.0.0";

    public static final String IMPORT_SPEC_V1 = "1.0.0";

    public static final String V1 = "1.0.0";

    public static final String V1_1 = "1.1.0";

    public static final String LATEST = Version.V1_1;

    private static final List<String> GRAPH_SPEC_VERSIONS = Kt.listOf(Version.V1, Version.LATEST);

    public static final String SCHEMA_BASE = "https://neo4j.com/docs/ontology-graph-spec/";

    public static final String SCHEMA_FILE = "schema.json";

    private Version() {
    }

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
    public static List<String> getGRAPH_SPEC_VERSIONS() {
        return Version.GRAPH_SPEC_VERSIONS;
    }

    public static String schemaUrl(String version) {
        return "" + Version.SCHEMA_BASE + version + "/" + Version.SCHEMA_FILE;
    }

    public static String parseSchemaVersion(String schema) {
        String version = Kt.substringAfterLast(Kt.trimEnd(Kt.removeSuffix(Kt.trimEnd(Kt.trim(schema), '/'), Version.SCHEMA_FILE), '/'), '/');
        if (!Kt.isNotBlank(version)) {
            throw new IllegalArgumentException(String.valueOf("Unable to determine version from schema '" + schema + "'"));
        }
        return version;
    }
}
