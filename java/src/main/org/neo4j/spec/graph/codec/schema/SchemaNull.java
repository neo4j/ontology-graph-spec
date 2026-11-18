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
