/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/DataModelUtils.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;

public final class DataModelUtils {
    private DataModelUtils() {}

    public static String ref(SchemaMap schemaMap) {
        return removeHash(schemaMap.string("$ref"));
    }

    public static String ref(SchemaMap schemaMap, String key) {
        return DataModelUtils.ref(schemaMap.map(key));
    }

    public static String id(SchemaMap schemaMap) {
        return schemaMap.string("$id");
    }

    static String removeHash(String string) {
        return string.startsWith("#") ? string.substring(1) : string;
    }

    /**
     * Builds a map from alternating keys and values, leaving out entries whose value is null.
     */
    static SchemaMap schemaMapOf(Object... keysAndValues) {
        Map<String, SchemaElement> content = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            Object value = keysAndValues[i + 1];
            if (value != null) {
                content.put((String) keysAndValues[i], SchemaElements.toSchemaElement(value));
            }
        }
        return new SchemaMap(content);
    }

    /**
     * Null for a null or empty map, so that it is left out by {@link #schemaMapOf}.
     */
    static <T extends Map<?, ?>> T nullIfEmpty(T map) {
        return map == null || map.isEmpty() ? null : map;
    }

    /**
     * Null for a null or empty collection, so that it is left out by {@link #schemaMapOf}.
     */
    static <T extends Collection<?>> T nullIfEmpty(T collection) {
        return collection == null || collection.isEmpty() ? null : collection;
    }

    /**
     * The {@code $ref} targets of the maps.
     */
    static List<String> refs(Collection<SchemaMap> schemaMaps) {
        List<String> result = new ArrayList<>();
        for (SchemaMap schemaMap : schemaMaps) {
            result.add(ref(schemaMap));
        }
        return result;
    }

    /**
     * A {@code $ref} map for each of the ids.
     */
    static List<SchemaMap> refsOf(Collection<String> ids) {
        List<SchemaMap> result = new ArrayList<>();
        for (String id : ids) {
            result.add(refOf(id));
        }
        return result;
    }

    /**
     * The string itself, or null when it is null or blank.
     */
    static String nullIfBlank(String string) {
        if (string == null) {
            return null;
        }
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
                return string;
            }
        }
        return null;
    }

    public static SchemaMap refOf(String id) {
        return schemaMapOf("$ref", "#" + removeHash(id));
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
