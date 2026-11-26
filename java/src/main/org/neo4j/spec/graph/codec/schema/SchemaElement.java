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
package org.neo4j.spec.graph.codec.schema;

/**
 * A format agnostic AST used for transforms and migrations.
 *
 * Elements know their location in the tree via a link to their {@code parent} plus their own
 * {@code identifier}. {@code path} is derived from that chain on demand to keep the memory footprint
 * down to a minimum and ensure that {@code repath} doesn't require a deep copy of the subtree.
 */
public interface SchemaElement {
    /**
     * The element that owns this one, or null when this element is a tree root.
     */
    SchemaElement getParent();

    void setParent(SchemaElement value);

    /**
     * This element's location relative to {@code parent}: a key for maps, `{@code i}` for lists.
     */
    String getIdentifier();

    void setIdentifier(String value);

    default String getPath() {
        SchemaElement receiver = getParent();
        String prefix = receiver == null ? null : receiver.getPath();
        if (prefix == null || prefix.isEmpty()) {
            return getIdentifier();
        } else if (getIdentifier().startsWith("[")) {
            return prefix + getIdentifier();
        } else {
            return prefix + "." + getIdentifier();
        }
    }

    /**
     * Re-roots this element at {@code newPath}. Descendants follow via their parent links.
     */
    SchemaElement repath(String newPath);

    @Override
    String toString();
}
