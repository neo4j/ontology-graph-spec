package org.neo4j.spec.graph.spectests;

import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.Validations;

/** Entry points of the native Java library, as seen from Java. */
final class Api {
    private Api() {}

    static GraphSpec json() {
        return GraphSpec.json();
    }

    static GraphSpec yaml() {
        return GraphSpec.yaml();
    }

    static Map<String, List<Validation>> validationSets() {
        return Map.of(
            "core", Validations.core(),
            "labelled", Validations.labelled(),
            "consistent_schema", Validations.consistentSchema(),
            "mappable", Validations.mappable(),
            "bulk_importable", Validations.bulkImportable(),
            "well_constrained", Validations.wellConstrained(),
            "all", Validations.all());
    }
}
