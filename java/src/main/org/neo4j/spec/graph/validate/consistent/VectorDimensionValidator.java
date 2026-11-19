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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.IndexOptions;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.VectorIndexOptions;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks the {@code vector.dimensions} of a {@code VECTOR} index match the {@code dimension} of the indexed property,
 * when both are set. Reports {@code vector_index_dimension_mismatch} at the index's {@code options}, with the
 * {@code index} and {@code property} dimensions.
 */
public final class VectorDimensionValidator implements Validator {

    static final VectorDimensionValidator INSTANCE = new VectorDimensionValidator();

    private VectorDimensionValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Schema.Index index : Schema.indexes(model)) {
            IndexOptions options = index.options().orElse(null);
            if (index.type() != IndexType.VECTOR || !(options instanceof VectorIndexOptions)) {
                continue;
            }
            Integer dimensions = ((VectorIndexOptions) options).dimensions().orElse(null);
            if (dimensions == null) {
                continue;
            }
            for (String id : index.properties()) {
                Property property = index.available().get(id);
                Integer dimension =
                        property == null ? null : property.dimension().orElse(null);
                if (dimension != null && !dimension.equals(dimensions)) {
                    Map<String, String> details = new LinkedHashMap<>();
                    details.put("index", String.valueOf(dimensions));
                    details.put("property", String.valueOf(dimension));
                    issues.add(Issue.of(
                            "vector_index_dimension_mismatch",
                            "The VECTOR index '" + index.id() + "' has " + dimensions + " dimensions but the property '"
                                    + id + "' has " + dimension,
                            index.path() + ".options",
                            details));
                }
            }
        }
        return issues;
    }

    @Override
    public Set<Validator> dependsOn() {
        return Dependencies.core();
    }

    @Override
    public String toString() {
        return "VectorDimensionValidator";
    }
}
