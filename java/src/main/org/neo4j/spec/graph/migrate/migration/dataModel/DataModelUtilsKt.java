// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/DataModelUtils.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import java.util.Objects;
import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;

public final class DataModelUtilsKt {
    private DataModelUtilsKt() {
    }

    public static String ref(SchemaMap schemaMap) {
        return Kt.removePrefix(schemaMap.string("$ref"), "#");
    }

    public static String ref(SchemaMap schemaMap, String key) {
        return DataModelUtilsKt.ref(schemaMap.map(key));
    }

    public static String id(SchemaMap schemaMap) {
        return schemaMap.string("$id");
    }

    public static SchemaMap refOf(String id) {
        return SchemaElements.schemaMapOf(new Pair<>("$ref", "#" + Kt.removePrefix(id, "#")));
    }

    public static ConstraintType dataModelConstraintTypeFrom(String word) {
        if (Objects.equals(word, "uniqueness")) {
            return ConstraintType.UNIQUE;
        } else if (Objects.equals(word, "propertyExistence")) {
            return ConstraintType.EXISTS;
        } else if (Objects.equals(word, "propertyType")) {
            return ConstraintType.PROPERTY_TYPE;
        } else if (Objects.equals(word, "key")) {
            return ConstraintType.KEY;
        } else {
            return null;
        }
    }

    public static IndexType dataModelIndexTypeFrom(String word) {
        if (Objects.equals(word, "lookup")) {
            return IndexType.LOOKUP;
        } else if ((Objects.equals(word, "default")) || (Objects.equals(word, "range"))) {
            return IndexType.RANGE;
        } else if (Objects.equals(word, "fullText")) {
            return IndexType.FULLTEXT;
        } else if (Objects.equals(word, "point")) {
            return IndexType.POINT;
        } else if (Objects.equals(word, "text")) {
            return IndexType.TEXT;
        } else if (Objects.equals(word, "vector")) {
            return IndexType.VECTOR;
        } else {
            return null;
        }
    }
}
