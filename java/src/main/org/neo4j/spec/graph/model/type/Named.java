// Generated from org/neo4j/spec/graph/model/type/Named.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.type;

/**
 * Interface for parts of the model which have an optional name in the database, separate from their id.
 */
public interface Named {
    String getName();

    void setName(String value);
}
