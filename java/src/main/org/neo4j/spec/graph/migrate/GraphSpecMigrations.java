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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/** The built-in {@link Migration} steps and {@link VersionDetector} for the released graph spec versions. */
public final class GraphSpecMigrations {

    private static final String SCHEMA = "schema";

    private static final VersionDetector DETECTOR = GraphSpecMigrations::detect;

    private GraphSpecMigrations() {}

    /**
     * Detects graph spec documents from their {@code schema} URL. A released URL gives that version; any other text
     * gives {@link SpecVersion#LATEST} (core validation reports the unknown URL later); a missing or non-text
     * {@code schema} isn't recognised.
     */
    public static VersionDetector detector() {
        return DETECTOR;
    }

    /** The step from 1.0.0 to 1.1.0, which only sets the {@code schema} URL. */
    public static Migration v1_0_0ToV1_1_0() {
        return new SchemaRewrite(SpecVersion.V1_0_0, SpecVersion.V1_1_0);
    }

    /** The step from 1.1.0 to 1.0.0, which only sets the {@code schema} URL. */
    public static Migration v1_1_0ToV1_0_0() {
        return new SchemaRewrite(SpecVersion.V1_1_0, SpecVersion.V1_0_0);
    }

    /**
     * Returns a copy of the document with {@code schema} set to the URL of the given graph spec version. The field
     * keeps its position, or is added first if it was absent.
     *
     * @param document the document to copy
     * @param version a graph spec version
     */
    public static ObjectNode withSchema(ObjectNode document, DocumentVersion version) {
        String url = SpecVersion.schemaUrl(version);
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        if (!document.has(SCHEMA)) {
            result.put(SCHEMA, url);
        }
        Iterator<Map.Entry<String, JsonNode>> fields = document.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (SCHEMA.equals(field.getKey())) {
                result.put(SCHEMA, url);
            } else {
                result.set(field.getKey(), field.getValue());
            }
        }
        return result;
    }

    private static Optional<DocumentVersion> detect(ObjectNode document) {
        JsonNode schema = document.get(SCHEMA);
        if (schema == null || !schema.isTextual()) {
            return Optional.empty();
        }
        Optional<DocumentVersion> released = SpecVersion.fromSchemaUrl(schema.asText());
        return released.isPresent() ? released : Optional.of(SpecVersion.LATEST);
    }

    private static final class SchemaRewrite extends TreeMigration {

        SchemaRewrite(DocumentVersion from, DocumentVersion to) {
            super(from, to);
        }

        @Override
        protected ObjectNode transform(ObjectNode document) {
            return withSchema(document, to());
        }
    }
}
