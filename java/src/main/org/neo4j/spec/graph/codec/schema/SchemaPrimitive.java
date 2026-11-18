// Generated from org/neo4j/spec/graph/codec/schema/SchemaPrimitive.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.codec.schema;

public abstract class SchemaPrimitive implements SchemaElement {
    protected SchemaPrimitive() {
    }

    public abstract boolean isString();

    public abstract String getString();
}
