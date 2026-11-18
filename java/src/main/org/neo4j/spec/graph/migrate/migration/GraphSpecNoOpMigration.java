// Generated from org/neo4j/spec/graph/migrate/migration/GraphSpecNoOpMigration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration;

import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.Type;

/**
 * Migration between graph spec versions where the format did not change. Only the version is bumped, which
 * {@code org.neo4j.spec.graph.migrate.MigrationPath} stamps on the migrated model.
 */
public final class GraphSpecNoOpMigration extends Migration {
    public GraphSpecNoOpMigration(String from, String to) {
        super(Type.GRAPH_SPEC, from, Type.GRAPH_SPEC, to);
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        return schema;
    }
}
