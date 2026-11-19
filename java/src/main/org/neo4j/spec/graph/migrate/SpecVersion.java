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

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The released versions of the Neo4j Ontology Graph Specification, and their schema URLs. */
public final class SpecVersion {

    /** The first version of the specification. */
    public static final DocumentVersion V1_0_0 = DocumentVersion.of(DocumentType.GRAPH_SPEC, Version.of(1, 0, 0));

    /** Adds the optional {@code aliases} of properties. */
    public static final DocumentVersion V1_1_0 = DocumentVersion.of(DocumentType.GRAPH_SPEC, Version.of(1, 1, 0));

    /** The latest version, which {@link org.neo4j.spec.graph.model.GraphModel} represents. */
    public static final DocumentVersion LATEST = V1_1_0;

    private static final List<DocumentVersion> RELEASED = List.of(V1_0_0, V1_1_0);

    private static final String URL_PREFIX = "https://neo4j.com/docs/ontology-graph-spec/";
    private static final String URL_SUFFIX = "/schema.json";
    private static final Pattern URL =
            Pattern.compile(Pattern.quote(URL_PREFIX) + "([0-9]+\\.[0-9]+\\.[0-9]+)" + Pattern.quote(URL_SUFFIX));

    private SpecVersion() {}

    /** Every released version, oldest first. */
    public static List<DocumentVersion> released() {
        return RELEASED;
    }

    /** Whether the version is a released graph spec version. */
    public static boolean isReleased(DocumentVersion version) {
        return RELEASED.contains(version);
    }

    /**
     * The URL of the JSON Schema of a graph spec version, which documents of that version set as their
     * {@code schema}.
     *
     * @throws IllegalArgumentException if the version isn't of {@link DocumentType#GRAPH_SPEC}
     */
    public static String schemaUrl(DocumentVersion version) {
        if (!DocumentType.GRAPH_SPEC.equals(version.type())) {
            throw new IllegalArgumentException("Not a graph spec version: " + version);
        }
        return URL_PREFIX + version.version() + URL_SUFFIX;
    }

    /** The released graph spec version whose schema URL this is, if any. */
    public static Optional<DocumentVersion> fromSchemaUrl(String url) {
        if (url == null) {
            return Optional.empty();
        }
        Matcher matcher = URL.matcher(url);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        DocumentVersion version = DocumentVersion.of(DocumentType.GRAPH_SPEC, matcher.group(1));
        return isReleased(version) ? Optional.of(version) : Optional.empty();
    }
}
