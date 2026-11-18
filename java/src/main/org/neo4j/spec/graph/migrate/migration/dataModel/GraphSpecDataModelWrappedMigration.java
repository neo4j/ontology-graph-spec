// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/GraphSpecDataModelWrappedMigration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.internal.Pair;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;

/**
 * Graph Spec 4.0 -> wrapped data_model 3.0
 *
 * Direct so that the shortest migration path keeps the envelope fields the flat data model would drop.
 */
public final class GraphSpecDataModelWrappedMigration extends Migration {
    private final GraphSpecDataModelV3Migration dataModel;

    public GraphSpecDataModelWrappedMigration() {
        super(Type.GRAPH_SPEC, Version.V1_1, Type.DATA_MODEL_WRAPPED, Version.DATA_MODEL_V3_0);
        this.dataModel = new GraphSpecDataModelV3Migration();
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        SchemaMap model = this.dataModel.migrate(schema);
        model.putAll(SchemaElements.schemaMapOf(SchemaElements.toNotEmpty("visualisation", this.dataModel.convertVisualisation(schema)), new Pair<>("description", schema.literalOrNull("description"))));
        return DataModelWrapper.wrap(model);
    }
}
