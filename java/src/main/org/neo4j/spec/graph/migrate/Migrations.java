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

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/**
 * An immutable set of {@link Migration} steps and {@link VersionDetector}s, which converts documents between any two
 * versions the steps connect, forwards or backwards and across document types.
 */
public final class Migrations {

    private Migrations() {}

    /** The built-in steps between the released graph spec versions, and the graph spec version detector. */
    public static Migrations defaults() {
        throw new UnsupportedOperationException("TODO");
    }

    /** A builder starting empty. */
    public static Builder builder() {
        throw new UnsupportedOperationException("TODO");
    }

    /** A builder starting with these steps and detectors, to add more to. */
    public Builder toBuilder() {
        throw new UnsupportedOperationException("TODO");
    }

    /** The registered steps. */
    public List<Migration> migrations() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * The version of a document, from the first detector that recognises it.
     *
     * @throws org.neo4j.spec.graph.GraphSpecException if no detector recognises it
     */
    public DocumentVersion detect(ObjectNode document) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * The shortest chain of steps from one version to another, empty when they're the same.
     *
     * @throws org.neo4j.spec.graph.GraphSpecException if no chain connects them
     */
    public List<Migration> path(DocumentVersion from, DocumentVersion to) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Converts a document from one version to another along {@link #path}.
     *
     * @throws org.neo4j.spec.graph.GraphSpecException if no chain connects them
     */
    public ObjectNode migrate(ObjectNode document, DocumentVersion from, DocumentVersion to) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Builds {@link Migrations}. */
    public static final class Builder {

        private Builder() {}

        public Builder add(Migration migration) {
            throw new UnsupportedOperationException("TODO");
        }

        public Builder detector(VersionDetector detector) {
            throw new UnsupportedOperationException("TODO");
        }

        public Migrations build() {
            throw new UnsupportedOperationException("TODO");
        }
    }
}
