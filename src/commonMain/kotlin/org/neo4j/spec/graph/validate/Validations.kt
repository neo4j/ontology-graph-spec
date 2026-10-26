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

import org.neo4j.spec.graph.validate.length.StringLength
import org.neo4j.spec.graph.validate.mapping.MappingDependencies
import org.neo4j.spec.graph.validate.node.NodeConstraintCoverage
import org.neo4j.spec.graph.validate.node.NodeConstraints
import org.neo4j.spec.graph.validate.node.NodeIndexConstraintNameConflict
import org.neo4j.spec.graph.validate.node.NodeIndexOptions
import org.neo4j.spec.graph.validate.node.NodeIndexProperties
import org.neo4j.spec.graph.validate.node.NodeIndexesExists
import org.neo4j.spec.graph.validate.node.NodeLabel
import org.neo4j.spec.graph.validate.node.NodeLabelToken
import org.neo4j.spec.graph.validate.node.NodeMappingKey
import org.neo4j.spec.graph.validate.node.NodeMappingKeyType
import org.neo4j.spec.graph.validate.node.NodeProperties
import org.neo4j.spec.graph.validate.node.NodePropertyDuplicateName
import org.neo4j.spec.graph.validate.node.NodePropertyEmptyName
import org.neo4j.spec.graph.validate.node.NodePropertyPatternOneOf
import org.neo4j.spec.graph.validate.node.constraint.NodeCompositeConstraintPropertyType
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintDuplicatePropertySet
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintProperties
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceCompositeConflict
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceConstraint
import org.neo4j.spec.graph.validate.node.constraint.NodeKeyOverlap
import org.neo4j.spec.graph.validate.node.constraint.NodeTypeConstraint
import org.neo4j.spec.graph.validate.relationship.RelationshipConstraints
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexOptions
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexProperties
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexes
import org.neo4j.spec.graph.validate.relationship.RelationshipMappingKey
import org.neo4j.spec.graph.validate.relationship.RelationshipNodes
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyDuplicateName
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyEmptyName
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyPatternOneOf
import org.neo4j.spec.graph.validate.relationship.RelationshipTargetCount
import org.neo4j.spec.graph.validate.relationship.RelationshipType
import org.neo4j.spec.graph.validate.relationship.RelationshipTypeToken
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipExistenceConstraint
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipKeyOverlap
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipTypeConstraint
import org.neo4j.spec.graph.validate.table.TableColumnDuplicateName
import org.neo4j.spec.graph.validate.table.TableColumnEmptyName
import org.neo4j.spec.graph.validate.table.TableColumnType
import kotlin.js.JsExport
import kotlin.js.JsStatic

@JsExport
class Validations {
    companion object {
        // The rules a model must follow beyond its JSON Schema to be valid: references resolve, constraint and
        // index details are consistent, relationship counts make sense and text is within length limits.
        @JsStatic
        val core: Set<Validation> = setOf(
            NodeTypeConstraint,
            RelationshipTypeConstraint,
            NodeConstraints,
            RelationshipConstraints,
            NodeIndexesExists,
            NodeIndexOptions,
            RelationshipIndexes,
            RelationshipIndexOptions,
            NodeExistenceConstraint,
            RelationshipExistenceConstraint,
            RelationshipNodes,
            RelationshipTargetCount,
            NodePropertyPatternOneOf,
            RelationshipPropertyPatternOneOf,
            RelationshipMappingKey,
            StringLength
        )

        // Every node has an identifier label and every relationship a type.
        @JsStatic
        val labelled: List<Validation> = listOf(
            NodeLabel,
            RelationshipType
        )

        // Property names are set and unique within their node or relationship, indexes and constraints have
        // properties, composite constraints don't repeat a property set or overlap an existence constraint, and index
        // and constraint names are unique.
        @JsStatic
        val consistentSchema: List<Validation> = listOf(
            NodePropertyEmptyName,
            NodePropertyDuplicateName,
            RelationshipPropertyEmptyName,
            RelationshipPropertyDuplicateName,
            NodeIndexProperties,
            RelationshipIndexProperties,
            NodeConstraintProperties,
            NodeConstraintDuplicatePropertySet,
            NodeExistenceCompositeConflict,
            NodeIndexConstraintNameConflict
        )

        // Node mappings have a key, mapping ids are unique and dependencies resolve without cycles, and data source
        // table columns have a unique name and, outside local tables, a type.
        @JsStatic
        val mappable: List<Validation> = listOf(
            NodeMappingKey,
            MappingDependencies,
            TableColumnEmptyName,
            TableColumnDuplicateName,
            TableColumnType
        )

        // Neo4j bulk import restrictions: labels and types don't contain invalid characters and node mapping keys and
        // composite constraint properties must be valid types.
        @JsStatic
        val bulkImportable: List<Validation> = listOf(
            NodeLabelToken,
            RelationshipTypeToken,
            NodeMappingKeyType,
            NodeCompositeConstraintPropertyType
        )

        // Opinionated checks the format doesn't require: every node has properties and a constraint, and no UNIQUE
        // or EXISTS constraint repeats the properties of a KEY constraint.
        @JsStatic
        val wellConstrained: List<Validation> = listOf(
            NodeProperties,
            NodeConstraintCoverage,
            NodeKeyOverlap,
            RelationshipKeyOverlap
        )

        // Every built-in validator.
        @JsStatic
        val all: List<Validation> =
            (core + labelled + consistentSchema + mappable + bulkImportable + wellConstrained).distinct()
    }
}
