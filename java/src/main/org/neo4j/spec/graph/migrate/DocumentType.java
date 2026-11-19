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

/**
 * A kind of document that migrations can read or write, such as this specification ({@link #GRAPH_SPEC}). Other
 * formats, such as another tool's model format, can be given their own type and joined to the graph spec with
 * {@link Migration}s.
 */
public final class DocumentType {

    /** Documents following the Neo4j Ontology Graph Specification. */
    public static final DocumentType GRAPH_SPEC = new DocumentType("graph_spec");

    private final String name;

    private DocumentType(String name) {
        this.name = name;
    }

    /**
     * Returns the document type with the given name.
     *
     * @param name a stable identifier, such as {@code graph_spec}
     */
    public static DocumentType of(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A document type name can't be blank");
        }
        return GRAPH_SPEC.name.equals(name) ? GRAPH_SPEC : new DocumentType(name);
    }

    /** The stable identifier of this type. */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof DocumentType && name.equals(((DocumentType) o).name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
