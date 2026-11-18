// Generated from org/neo4j/spec/graph/model/extension/mapping/Mapping.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.extension.mapping;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonClassDescription("A mapping from a data source table to the graph. Either a node, relationship or query mapping.")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NodeMapping.class, name = "NODE"),
        @JsonSubTypes.Type(value = QueryMapping.class, name = "QUERY"),
        @JsonSubTypes.Type(value = RelationshipMapping.class, name = "RELATIONSHIP")
})
public interface Mapping {
}
