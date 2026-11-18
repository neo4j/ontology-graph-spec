// Generated from org/neo4j/spec/graph/codec/format/Format.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.codec.format;

import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.model.GraphModel;

public interface Format {
    String encodeToString(SchemaElement element);

    SchemaElement decodeFromString(String string);

    SchemaElement encodeToSchema(GraphModel model);

    GraphModel decodeFromSchema(SchemaElement element);

    default GraphModel decodeModelFromString(String content) {
        return decodeFromSchema(decodeFromString(content));
    }

    default String encodeModelToString(GraphModel model) {
        return encodeToString(encodeToSchema(model));
    }
}
