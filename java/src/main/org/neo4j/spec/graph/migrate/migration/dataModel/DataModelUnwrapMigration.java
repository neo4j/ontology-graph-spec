// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/DataModelUnwrapMigration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.Type;

/**
 * Wrapped -> flat data_model of the same version
 */
public final class DataModelUnwrapMigration extends Migration {
    public DataModelUnwrapMigration(String version) {
        super(Type.DATA_MODEL_WRAPPED, version, Type.DATA_MODEL, version);
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        return DataModelWrapper.unwrap(schema);
    }
}
