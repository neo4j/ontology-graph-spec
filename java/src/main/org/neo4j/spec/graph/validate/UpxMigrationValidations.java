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
package org.neo4j.spec.graph.validate;

import java.util.List;
import org.neo4j.spec.graph.validate.mapping.MappingDependencies;
import org.neo4j.spec.graph.validate.node.NodeConstraintCoverage;
import org.neo4j.spec.graph.validate.node.NodeIndexConstraintNameConflict;
import org.neo4j.spec.graph.validate.node.NodeIndexProperties;
import org.neo4j.spec.graph.validate.node.NodeLabel;
import org.neo4j.spec.graph.validate.node.NodeLabelToken;
import org.neo4j.spec.graph.validate.node.NodeMappingKey;
import org.neo4j.spec.graph.validate.node.NodeMappingKeyType;
import org.neo4j.spec.graph.validate.node.NodeProperties;
import org.neo4j.spec.graph.validate.node.NodePropertyDuplicateName;
import org.neo4j.spec.graph.validate.node.NodePropertyEmptyName;
import org.neo4j.spec.graph.validate.node.constraint.NodeCompositeConstraintPropertyType;
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintDuplicatePropertySet;
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintProperties;
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceCompositeConflict;
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceConstraint;
import org.neo4j.spec.graph.validate.node.constraint.NodeKeyOverlap;
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexProperties;
import org.neo4j.spec.graph.validate.relationship.RelationshipNodes;
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyDuplicateName;
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyEmptyName;
import org.neo4j.spec.graph.validate.relationship.RelationshipType;
import org.neo4j.spec.graph.validate.relationship.RelationshipTypeToken;
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipExistenceConstraint;
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipKeyOverlap;
import org.neo4j.spec.graph.validate.table.TableColumnDuplicateName;
import org.neo4j.spec.graph.validate.table.TableColumnEmptyName;
import org.neo4j.spec.graph.validate.table.TableColumnType;

public final class UpxMigrationValidations {
    private static final List<Validation> KG_BUILDER_COMPLETE = List.of(
            NodeLabel.INSTANCE,
            NodeProperties.INSTANCE,
            NodeConstraintCoverage.INSTANCE,
            NodeKeyOverlap.INSTANCE,
            NodeExistenceConstraint.INSTANCE,
            RelationshipType.INSTANCE,
            RelationshipKeyOverlap.INSTANCE,
            RelationshipExistenceConstraint.INSTANCE,
            RelationshipNodes.INSTANCE);

    private static final List<Validation> IMPORT_COMPLETE = List.of(
            NodeLabel.INSTANCE,
            RelationshipType.INSTANCE,
            NodeMappingKey.INSTANCE,
            NodeIndexProperties.INSTANCE,
            RelationshipIndexProperties.INSTANCE,
            NodeConstraintProperties.INSTANCE,
            NodeConstraintDuplicatePropertySet.INSTANCE,
            NodeExistenceCompositeConflict.INSTANCE,
            NodeIndexConstraintNameConflict.INSTANCE,
            TableColumnEmptyName.INSTANCE,
            TableColumnDuplicateName.INSTANCE,
            TableColumnType.INSTANCE,
            NodePropertyEmptyName.INSTANCE,
            NodePropertyDuplicateName.INSTANCE,
            RelationshipPropertyEmptyName.INSTANCE,
            RelationshipPropertyDuplicateName.INSTANCE,
            MappingDependencies.INSTANCE);

    private static final List<Validation> DRAFT = List.of(NodeLabel.INSTANCE, RelationshipType.INSTANCE);

    private static final List<Validation> BULK_IMPORT_COMPLETE = List.of(
            NodeLabelToken.INSTANCE,
            RelationshipTypeToken.INSTANCE,
            NodeMappingKeyType.INSTANCE,
            NodeCompositeConstraintPropertyType.INSTANCE);

    public UpxMigrationValidations() {}

    public static List<Validation> kgBuilderComplete() {
        return UpxMigrationValidations.KG_BUILDER_COMPLETE;
    }

    public static List<Validation> importComplete() {
        return UpxMigrationValidations.IMPORT_COMPLETE;
    }

    public static List<Validation> draft() {
        return UpxMigrationValidations.DRAFT;
    }

    public static List<Validation> bulkImportComplete() {
        return UpxMigrationValidations.BULK_IMPORT_COMPLETE;
    }
}
