// Generated from org/neo4j/spec/graph/model/index/IndexOption.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.index;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.neo4j.spec.graph.model.type.IndexType;

@JsonClassDescription("Configuration specific to the type of an index.")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FullTextIndexOption.class, name = "FULLTEXT"),
        @JsonSubTypes.Type(value = PointIndexOption.class, name = "POINT"),
        @JsonSubTypes.Type(value = VectorIndexOption.class, name = "VECTOR")
})
public interface IndexOption {
    IndexType getType();
}
