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
package org.neo4j.spec.graph.validate.core;

import java.util.List;
import org.neo4j.spec.graph.migrate.SpecVersion;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks the model's {@code schema} is the schema URL of a released version of the specification. Reports
 * {@code unknown_schema} at {@code schema}.
 */
public final class SchemaUrlValidator implements Validator {

    static final SchemaUrlValidator INSTANCE = new SchemaUrlValidator();

    private SchemaUrlValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        if (SpecVersion.fromSchemaUrl(model.schema()).isPresent()) {
            return List.of();
        }
        return List.of(Issue.of(
                "unknown_schema",
                "The schema '" + model.schema() + "' isn't the schema URL of a released version of the specification",
                "schema"));
    }

    @Override
    public String toString() {
        return "SchemaUrlValidator";
    }
}
