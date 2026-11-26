package org.neo4j.spec.graph.spectests;

import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.Validations;

/** Entry points of the Kotlin library, as seen from Java. */
final class Api {
    private Api() {}

    static GraphSpec json() {
        return GraphSpec.getJson();
    }

    static GraphSpec yaml() {
        return GraphSpec.getYaml();
    }

    static Map<String, List<Validation>> validationSets() {
        return Map.of(
            "core", Validations.getCore(),
            "labelled", Validations.getLabelled(),
            "consistent_schema", Validations.getConsistentSchema(),
            "mappable", Validations.getMappable(),
            "bulk_importable", Validations.getBulkImportable(),
            "well_constrained", Validations.getWellConstrained(),
            "all", Validations.getAll());
    }
}
