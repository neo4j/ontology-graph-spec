// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/DataModelWrapMigration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;

/**
 * Flat -> wrapped data_model 3.0
 */
public final class DataModelWrapMigration extends Migration {
    public DataModelWrapMigration() {
        super(Type.DATA_MODEL, Version.DATA_MODEL_V3_0, Type.DATA_MODEL_WRAPPED, Version.DATA_MODEL_V3_0);
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        return DataModelWrapper.wrap(schema);
    }
}
