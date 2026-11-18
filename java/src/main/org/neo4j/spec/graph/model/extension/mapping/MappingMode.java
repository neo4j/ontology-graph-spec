// Generated from org/neo4j/spec/graph/model/extension/mapping/MappingMode.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.extension.mapping;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Write approach used when importing data (merged into existing data or always created).")
public enum MappingMode {
    MERGE,

    CREATE;

    MappingMode() {
    }
}
