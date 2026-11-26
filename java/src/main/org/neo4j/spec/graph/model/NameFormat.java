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
package org.neo4j.spec.graph.model;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * Builds constraint and index names in the importer's expected form, {@code <property>_<label>_<suffix>}.
 */
public final class NameFormat {
    private static final int MAX_LENGTH = 800;

    public NameFormat() {}

    public static String constraintName(List<String> properties, String entity, ConstraintType type) {
        return NameFormat.build(properties, entity, NameFormat.suffixFrom(type));
    }

    public static String indexName(List<String> properties, String entity, IndexType type) {
        return NameFormat.build(properties, entity, NameFormat.suffixFrom(type));
    }

    public static String indexName(List<String> properties, String entity) {
        return NameFormat.indexName(properties, entity, null);
    }

    private static String build(List<String> properties, String entity, String suffix) {
        String propertyPart = properties.stream().map(NameFormat::escape).collect(Collectors.joining("_"));
        if (propertyPart.isEmpty()) {
            return "";
        }
        String base = entity.isEmpty() ? propertyPart : propertyPart + "_" + NameFormat.escape(entity);
        String suffixPart = suffix == null ? "" : "_" + suffix;
        return base.substring(0, Math.min(Math.max(NameFormat.MAX_LENGTH - suffixPart.length(), 0), base.length()))
                + suffixPart;
    }

    private static String suffixFrom(ConstraintType type) {
        if (type == ConstraintType.UNIQUE) {
            return "uniq";
        } else if (type == ConstraintType.KEY) {
            return "key";
        } else if (type == ConstraintType.EXISTS) {
            return "exists";
        } else if (type == ConstraintType.PROPERTY_TYPE) {
            return "type";
        } else {
            throw new IllegalStateException("Unexpected when branch");
        }
    }

    private static String suffixFrom(IndexType type) {
        String receiver = type == null ? null : type.name();
        return receiver == null ? null : receiver.toLowerCase(Locale.ROOT);
    }

    /**
     * An underscore inside a token is doubled, so the parts stay recoverable. Without it `a_b` on label
     * `c` collides with `a` on label `b_c`. A token that starts or ends with an underscore is still
     * ambiguous.
     */
    private static String escape(String token) {
        return token.replace("_", "__");
    }
}
