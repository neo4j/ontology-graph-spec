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
// Generated from org/neo4j/spec/graph/codec/schema/SchemaNull.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.codec.schema;

import java.util.Objects;

public final class SchemaNull extends SchemaPrimitive {
    private SchemaElement parent;

    private String identifier;

    private final String string;

    public SchemaNull(String path) {
        super();
        this.parent = null;
        this.identifier = path;
        this.string = "null";
    }

    public SchemaNull() {
        this("");
    }

    @Override
    public SchemaElement getParent() {
        return this.parent;
    }

    public void setParent(SchemaElement value) {
        this.parent = value;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String value) {
        this.identifier = value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SchemaNull;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toString());
    }

    @Override
    public SchemaElement repath(String newPath) {
        return SchemaElements.reroot(this, newPath);
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public String getString() {
        return this.string;
    }

    @Override
    public boolean isString() {
        return false;
    }
}
