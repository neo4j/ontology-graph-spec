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
package org.neo4j.spec.graph.validate.consistent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.model.Neo4jType;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks the types of indexed properties suit the type of the index, for properties that have a type. A {@code TEXT}
 * index needs {@code STRING}, {@code FULLTEXT} needs {@code STRING} or {@code LIST<STRING>}, {@code POINT} needs
 * {@code POINT}, and {@code VECTOR} needs a {@code VECTOR<...>} type or a list of floats or integers. {@code RANGE}
 * and {@code LOOKUP} accept any type. Reports {@code node_index_property_type_mismatch} and
 * {@code relationship_index_property_type_mismatch} at the indexed property, with the {@code index_type} and the
 * {@code property_type}.
 */
public final class IndexPropertyTypeValidator implements Validator {

    static final IndexPropertyTypeValidator INSTANCE = new IndexPropertyTypeValidator();

    private static final Set<Neo4jType> TEXT = EnumSet.of(Neo4jType.STRING);
    private static final Set<Neo4jType> FULLTEXT = EnumSet.of(Neo4jType.STRING, Neo4jType.LIST_STRING);
    private static final Set<Neo4jType> POINT = EnumSet.of(Neo4jType.POINT);
    private static final Set<Neo4jType> VECTOR = EnumSet.of(
            Neo4jType.VECTOR_FLOAT,
            Neo4jType.VECTOR_FLOAT32,
            Neo4jType.VECTOR_INTEGER,
            Neo4jType.VECTOR_INTEGER32,
            Neo4jType.VECTOR_INTEGER16,
            Neo4jType.VECTOR_INTEGER8,
            Neo4jType.LIST_FLOAT,
            Neo4jType.LIST_FLOAT32,
            Neo4jType.LIST_INTEGER,
            Neo4jType.LIST_INTEGER32,
            Neo4jType.LIST_INTEGER16,
            Neo4jType.LIST_INTEGER8);

    private IndexPropertyTypeValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Schema.Index index : Schema.indexes(model)) {
            Set<Neo4jType> accepted = accepted(index.type());
            if (accepted == null) {
                continue;
            }
            for (int i = 0; i < index.properties().size(); i++) {
                String id = index.properties().get(i);
                Property property = index.available().get(id);
                if (property == null
                        || !property.type().isPresent()
                        || accepted.contains(property.type().get())) {
                    continue;
                }
                String type = property.type().get().value();
                Map<String, String> details = new LinkedHashMap<>();
                details.put("index_type", index.type().value());
                details.put("property_type", type);
                issues.add(Issue.of(
                        index.owner() + "_index_property_type_mismatch",
                        "The " + index.type().value() + " index '" + index.id() + "' can't index the property '" + id
                                + "' of type " + type,
                        index.path() + ".properties[" + i + "]",
                        details));
            }
        }
        return issues;
    }

    /** The property types an index type accepts, or {@code null} for any. */
    private static Set<Neo4jType> accepted(IndexType type) {
        switch (type) {
            case TEXT:
                return TEXT;
            case FULLTEXT:
                return FULLTEXT;
            case POINT:
                return POINT;
            case VECTOR:
                return VECTOR;
            default:
                return null;
        }
    }

    @Override
    public Set<Validator> dependsOn() {
        return Dependencies.core();
    }

    @Override
    public String toString() {
        return "IndexPropertyTypeValidator";
    }
}
