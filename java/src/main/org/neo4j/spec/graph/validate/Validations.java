// Generated from org/neo4j/spec/graph/validate/Validations.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.neo4j.spec.graph.validate.length.StringLength;
import org.neo4j.spec.graph.validate.mapping.MappingDependencies;
import org.neo4j.spec.graph.validate.node.NodeAliasDuplicate;
import org.neo4j.spec.graph.validate.node.NodeConstraintCoverage;
import org.neo4j.spec.graph.validate.node.NodeConstraints;
import org.neo4j.spec.graph.validate.node.NodeIndexConstraintNameConflict;
import org.neo4j.spec.graph.validate.node.NodeIndexLabelDuplicate;
import org.neo4j.spec.graph.validate.node.NodeIndexOptions;
import org.neo4j.spec.graph.validate.node.NodeIndexProperties;
import org.neo4j.spec.graph.validate.node.NodeIndexPropertyDuplicate;
import org.neo4j.spec.graph.validate.node.NodeIndexesExists;
import org.neo4j.spec.graph.validate.node.NodeLabel;
import org.neo4j.spec.graph.validate.node.NodeLabelDuplicate;
import org.neo4j.spec.graph.validate.node.NodeLabelToken;
import org.neo4j.spec.graph.validate.node.NodeMappingKey;
import org.neo4j.spec.graph.validate.node.NodeMappingKeyDuplicate;
import org.neo4j.spec.graph.validate.node.NodeMappingKeyType;
import org.neo4j.spec.graph.validate.node.NodeProperties;
import org.neo4j.spec.graph.validate.node.NodePropertyAliasDuplicate;
import org.neo4j.spec.graph.validate.node.NodePropertyDuplicateName;
import org.neo4j.spec.graph.validate.node.NodePropertyEmptyName;
import org.neo4j.spec.graph.validate.node.NodePropertyPatternOneOf;
import org.neo4j.spec.graph.validate.node.constraint.NodeCompositeConstraintPropertyType;
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintDuplicatePropertySet;
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintProperties;
import org.neo4j.spec.graph.validate.node.constraint.NodeConstraintPropertyDuplicate;
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceCompositeConflict;
import org.neo4j.spec.graph.validate.node.constraint.NodeExistenceConstraint;
import org.neo4j.spec.graph.validate.node.constraint.NodeKeyOverlap;
import org.neo4j.spec.graph.validate.node.constraint.NodeTypeConstraint;
import org.neo4j.spec.graph.validate.relationship.RelationshipAliasDuplicate;
import org.neo4j.spec.graph.validate.relationship.RelationshipConstraints;
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexOptions;
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexProperties;
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexPropertyDuplicate;
import org.neo4j.spec.graph.validate.relationship.RelationshipIndexes;
import org.neo4j.spec.graph.validate.relationship.RelationshipMappingKey;
import org.neo4j.spec.graph.validate.relationship.RelationshipMappingKeyDuplicate;
import org.neo4j.spec.graph.validate.relationship.RelationshipNodes;
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyAliasDuplicate;
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyDuplicateName;
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyEmptyName;
import org.neo4j.spec.graph.validate.relationship.RelationshipPropertyPatternOneOf;
import org.neo4j.spec.graph.validate.relationship.RelationshipTargetCount;
import org.neo4j.spec.graph.validate.relationship.RelationshipType;
import org.neo4j.spec.graph.validate.relationship.RelationshipTypeToken;
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipConstraintDuplicatePropertySet;
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipConstraintPropertyDuplicate;
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipExistenceConstraint;
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipKeyOverlap;
import org.neo4j.spec.graph.validate.relationship.constraint.RelationshipTypeConstraint;
import org.neo4j.spec.graph.validate.table.TableColumnDuplicateName;
import org.neo4j.spec.graph.validate.table.TableColumnEmptyName;
import org.neo4j.spec.graph.validate.table.TableColumnSupportedDuplicate;
import org.neo4j.spec.graph.validate.table.TableColumnType;

public final class Validations {
    private static final List<Validation> core = List.of(NodeTypeConstraint.INSTANCE, RelationshipTypeConstraint.INSTANCE, NodeConstraints.INSTANCE, RelationshipConstraints.INSTANCE, NodeIndexesExists.INSTANCE, NodeIndexOptions.INSTANCE, RelationshipIndexes.INSTANCE, RelationshipIndexOptions.INSTANCE, NodeExistenceConstraint.INSTANCE, RelationshipExistenceConstraint.INSTANCE, RelationshipNodes.INSTANCE, RelationshipTargetCount.INSTANCE, NodePropertyPatternOneOf.INSTANCE, RelationshipPropertyPatternOneOf.INSTANCE, RelationshipMappingKey.INSTANCE, StringLength.INSTANCE, NodeAliasDuplicate.INSTANCE, RelationshipAliasDuplicate.INSTANCE, NodePropertyAliasDuplicate.INSTANCE, RelationshipPropertyAliasDuplicate.INSTANCE, NodeLabelDuplicate.INSTANCE, NodeIndexLabelDuplicate.INSTANCE, NodeIndexPropertyDuplicate.INSTANCE, RelationshipIndexPropertyDuplicate.INSTANCE, NodeConstraintPropertyDuplicate.INSTANCE, RelationshipConstraintPropertyDuplicate.INSTANCE, NodeMappingKeyDuplicate.INSTANCE, RelationshipMappingKeyDuplicate.INSTANCE, TableColumnSupportedDuplicate.INSTANCE);

    private static final List<Validation> labelled = List.of(NodeLabel.INSTANCE, RelationshipType.INSTANCE);

    private static final List<Validation> consistentSchema = List.of(NodePropertyEmptyName.INSTANCE, NodePropertyDuplicateName.INSTANCE, RelationshipPropertyEmptyName.INSTANCE, RelationshipPropertyDuplicateName.INSTANCE, NodeIndexProperties.INSTANCE, RelationshipIndexProperties.INSTANCE, NodeConstraintProperties.INSTANCE, NodeConstraintDuplicatePropertySet.INSTANCE, RelationshipConstraintDuplicatePropertySet.INSTANCE, NodeExistenceCompositeConflict.INSTANCE, NodeIndexConstraintNameConflict.INSTANCE);

    private static final List<Validation> mappable = List.of(NodeMappingKey.INSTANCE, MappingDependencies.INSTANCE, TableColumnEmptyName.INSTANCE, TableColumnDuplicateName.INSTANCE, TableColumnType.INSTANCE);

    private static final List<Validation> bulkImportable = List.of(NodeLabelToken.INSTANCE, RelationshipTypeToken.INSTANCE, NodeMappingKeyType.INSTANCE, NodeCompositeConstraintPropertyType.INSTANCE);

    private static final List<Validation> wellConstrained = List.of(NodeProperties.INSTANCE, NodeConstraintCoverage.INSTANCE, NodeKeyOverlap.INSTANCE, RelationshipKeyOverlap.INSTANCE);

    private static final List<Validation> all = List.copyOf(Stream.of(core, labelled, consistentSchema, mappable, bulkImportable, wellConstrained)
            .flatMap(List::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new)));

    public Validations() {
    }

    public static List<Validation> getCore() {
        return Validations.core;
    }

    public static List<Validation> getLabelled() {
        return Validations.labelled;
    }

    public static List<Validation> getConsistentSchema() {
        return Validations.consistentSchema;
    }

    public static List<Validation> getMappable() {
        return Validations.mappable;
    }

    public static List<Validation> getBulkImportable() {
        return Validations.bulkImportable;
    }

    public static List<Validation> getWellConstrained() {
        return Validations.wellConstrained;
    }

    public static List<Validation> getAll() {
        return Validations.all;
    }
}
