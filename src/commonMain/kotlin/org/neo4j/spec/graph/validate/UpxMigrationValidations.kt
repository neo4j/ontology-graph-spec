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
package org.neo4j.spec.graph.validate

import org.neo4j.spec.graph.validate.mapping.MappingDependencies
import org.neo4j.spec.graph.validate.node.NodeConstraintCoverage
import org.neo4j.spec.graph.validate.node.NodeIndexConstraintNameConflict
import org.neo4j.spec.graph.validate.node.NodeIndexProperties
import org.neo4j.spec.graph.validate.node.NodeLabel
import org.neo4j.spec.graph.validate.node.NodeLabelToken
import org.neo4j.spec.graph.validate.node.NodeMappingKey
import org.neo4j.spec.graph.validate.node.NodeMappingKeyType
import org.neo4j.spec.graph.validate.node.NodeProperties
import org.neo4j.spec.graph.validate.node.NodePropertyDuplicateName
import org.neo4j.spec.graph.validate.node.NodePropertyEmptyName
import org.neo4j.spec.graph.validate.node.constraint.NodeCompositeConstraintPropertyType
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintDuplicatePropertySet
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintProperties
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceCompositeConflict
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceConstraint
import org.neo4j.spec.graph.validate.node.constraint.NodeKeyOverlap
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexProperties
import org.neo4j.spec.graph.validate.relationship.RelationshipNodes
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyDuplicateName
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyEmptyName
import org.neo4j.spec.graph.validate.relationship.RelationshipType
import org.neo4j.spec.graph.validate.relationship.RelationshipTypeToken
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipExistenceConstraint
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipKeyOverlap
import org.neo4j.spec.graph.validate.table.TableColumnDuplicateName
import org.neo4j.spec.graph.validate.table.TableColumnEmptyName
import org.neo4j.spec.graph.validate.table.TableColumnType
import kotlin.js.JsExport
import kotlin.js.JsStatic

// TEMPORARY: these sets exist only to ease UPX's migration onto graph-spec validation, and will be removed once UPX
// has moved to the functional sets in Validations. Each one keeps the exact validators and order of an existing UPX
// call site so issues don't change while that happens.
//
// Don't use these anywhere else, including the other language libraries, and don't add new sets here. New sets belong
// in Validations, named after what they check rather than who calls them.
@JsExport
class UpxMigrationValidations {
    companion object {
        // UPX kg-builder `validateStructuredSchema` (schemas-validators/) - gates accepting
        // an AI-generated schema before it's applied to the model.
        //
        // Validations.labelled + Validations.wellConstrained, plus NodeExistenceConstraint,
        // RelationshipExistenceConstraint and RelationshipNodes from Validations.core.
        @JsStatic
        val kgbuilderComplete: List<Validation> = listOf(
            NodeLabel,
            NodeProperties,
            NodeConstraintCoverage,
            NodeKeyOverlap,
            NodeExistenceConstraint,
            RelationshipType,
            RelationshipKeyOverlap,
            RelationshipExistenceConstraint,
            RelationshipNodes
        )

        // UPX `getDataModelErrors` (errors.ts) - shared call site, gates "Run Import"
        // in both kg-builder's data-model-slice.ts and import's data-model.ts.
        //
        // Validations.labelled + Validations.consistentSchema + Validations.mappable.
        @JsStatic
        val importComplete: List<Validation> = listOf(
            NodeLabel,
            RelationshipType,
            NodeMappingKey,
            NodeIndexProperties,
            RelationshipIndexProperties,
            NodeConstraintProperties,
            NodeConstraintDuplicatePropertySet,
            NodeExistenceCompositeConflict,
            NodeIndexConstraintNameConflict,
            TableColumnEmptyName,
            TableColumnDuplicateName,
            TableColumnType,
            NodePropertyEmptyName,
            NodePropertyDuplicateName,
            RelationshipPropertyEmptyName,
            RelationshipPropertyDuplicateName,
            MappingDependencies
        )

        // UPX `migrateDataModelToLatestVersion` (migrations.ts) - throws and aborts loading a
        // model, called by both import and ontologies whenever a saved model is loaded/uploaded.
        //
        // Validations.labelled.
        @JsStatic
        val draft: List<Validation> = listOf(
            NodeLabel,
            RelationshipType
        )

        // UPX `apps/import/.../data-model.utils.ts` - import-app-only bulk pass.
        //
        // Validations.bulkImportable.
        @JsStatic
        val bulkImportComplete: List<Validation> = listOf(
            NodeLabelToken,
            RelationshipTypeToken,
            NodeMappingKeyType,
            NodeCompositeConstraintPropertyType
        )
    }
}
