// Generated from org/neo4j/spec/graph/migrate/Migration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate;

import org.neo4j.spec.graph.codec.schema.SchemaMap;

public abstract class Migration {
    private final String fromType;

    private final String from;

    private final String toType;

    private final String to;

    protected Migration(String fromType, String from, String toType, String to) {
        this.fromType = fromType;
        this.from = from;
        this.toType = toType;
        this.to = to;
    }

    public String getFromType() {
        return this.fromType;
    }

    public String getFrom() {
        return this.from;
    }

    public String getToType() {
        return this.toType;
    }

    public String getTo() {
        return this.to;
    }

    public String getFromKey() {
        return getFromType() + ":" + getFrom();
    }

    public String getToKey() {
        return getToType() + ":" + getTo();
    }

    public abstract SchemaMap migrate(SchemaMap schema);
}
