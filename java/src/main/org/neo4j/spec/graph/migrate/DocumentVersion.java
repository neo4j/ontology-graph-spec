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

import java.util.Objects;

/** A version of a {@link DocumentType}, such as graph spec 1.1.0. See {@link SpecVersion} for the graph spec ones. */
public final class DocumentVersion {

    private final DocumentType type;
    private final Version version;

    private DocumentVersion(DocumentType type, Version version) {
        this.type = Objects.requireNonNull(type, "type");
        this.version = Objects.requireNonNull(version, "version");
    }

    public static DocumentVersion of(DocumentType type, Version version) {
        return new DocumentVersion(type, version);
    }

    public static DocumentVersion of(DocumentType type, String version) {
        return new DocumentVersion(type, Version.parse(version));
    }

    public DocumentType type() {
        return type;
    }

    public Version version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentVersion)) {
            return false;
        }
        DocumentVersion other = (DocumentVersion) o;
        return type.equals(other.type) && version.equals(other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, version);
    }

    @Override
    public String toString() {
        return type + " " + version;
    }
}
