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
import java.util.Objects;

/**
 * Base class for a {@link Migration} that transforms the document tree. A step only needs to say which versions it
 * connects and implement {@link #transform}.
 */
public abstract class TreeMigration implements Migration {

    private final DocumentVersion from;
    private final DocumentVersion to;

    /**
     * Creates a step between two versions.
     *
     * @param from the version read
     * @param to the version written
     */
    protected TreeMigration(DocumentVersion from, DocumentVersion to) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }

    @Override
    public final DocumentVersion from() {
        return from;
    }

    @Override
    public final DocumentVersion to() {
        return to;
    }

    @Override
    public final ObjectNode apply(ObjectNode document) {
        return transform(document);
    }

    /**
     * Converts a document of version {@link #from()} to {@link #to()}. May modify the input or return a new tree.
     *
     * @param document a document of version {@link #from()}
     * @return the document as version {@link #to()}
     */
    protected abstract ObjectNode transform(ObjectNode document);

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
