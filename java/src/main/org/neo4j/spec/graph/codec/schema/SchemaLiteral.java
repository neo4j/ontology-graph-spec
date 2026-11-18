// Generated from org/neo4j/spec/graph/codec/schema/SchemaLiteral.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.codec.schema;

import java.util.Objects;

public final class SchemaLiteral extends SchemaPrimitive {
    private final String string;

    private final boolean isString;

    private SchemaElement parent;

    private String identifier;

    public SchemaLiteral(String string, String path, boolean isString) {
        super();
        this.string = string;
        this.isString = isString;
        this.parent = null;
        this.identifier = path;
    }

    public SchemaLiteral(String string, String path) {
        this(string, path, true);
    }

    public SchemaLiteral(String string) {
        this(string, "", true);
    }

    @Override
    public String getString() {
        return this.string;
    }

    @Override
    public boolean isString() {
        return this.isString;
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
    public SchemaElement repath(String newPath) {
        return SchemaElements.reroot(this, newPath);
    }

    @Override
    public String toString() {
        return getString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !Objects.equals(this.getClass(), other.getClass())) {
            return false;
        }
        return Objects.equals(getString(), ((SchemaLiteral) ((SchemaLiteral) other)).getString());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getString());
    }
}
