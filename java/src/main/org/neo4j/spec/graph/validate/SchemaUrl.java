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
package org.neo4j.spec.graph.validate;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Version;

/**
 * The model's {@code schema} url must be the url of a released graph spec version. Surrounding whitespace, trailing
 * slashes and a missing {@code schema.json} are tolerated, as they are by the version parsing.
 */
public final class SchemaUrl implements Validation {
    public static final SchemaUrl INSTANCE = new SchemaUrl();

    private SchemaUrl() {}

    @Override
    public void validate(GraphModel model, List<Issue> issues) {
        String schema = model.getSchema();
        String base = stripTrailingSlashes(schema.trim());
        if (base.endsWith(Version.SCHEMA_FILE)) {
            base = base.substring(0, base.length() - Version.SCHEMA_FILE.length());
        }
        base = stripTrailingSlashes(base);
        for (String version : Version.GRAPH_SPEC_VERSIONS()) {
            if ((Version.SCHEMA_BASE + version).equals(base)) {
                return;
            }
        }
        issues.add(new Issue(
                "invalid_schema_url",
                "Schema '" + schema + "' is not the url of a released graph spec schema",
                Issue.jsonPointerOf("schema")));
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
