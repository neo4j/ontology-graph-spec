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
package org.neo4j.spec.graph.validate.consistent;

import java.util.List;
import org.neo4j.spec.graph.validate.Validator;

/**
 * The validators that make up {@link org.neo4j.spec.graph.validate.Validators#consistentSchema()}, to pick
 * individually or combine with {@link org.neo4j.spec.graph.validate.ValidatorSet#of(Validator...)}. Each depends on
 * {@code CoreValidators.REFERENCES}, {@code CoreValidators.CONSTRAINT_PROPERTY_COUNTS} and
 * {@code CoreValidators.INDEX_OPTIONS}, so dangling ids are reported once, by the core set.
 */
public final class ConsistentSchemaValidators {

    /** Checks constraints and indexes have a name. */
    public static final Validator SCHEMA_NAMES = SchemaNameValidator.INSTANCE;

    /** Checks constraint and index names are unique, as they share one namespace in Neo4j. */
    public static final Validator UNIQUE_SCHEMA_NAMES = SchemaNameUniquenessValidator.INSTANCE;

    /** Checks constraints aren't duplicated. */
    public static final Validator DUPLICATE_CONSTRAINTS = DuplicateConstraintValidator.INSTANCE;

    /** Checks indexes aren't duplicated. */
    public static final Validator DUPLICATE_INDEXES = DuplicateIndexValidator.INSTANCE;

    /** Checks range indexes don't clash with the index behind a unique or key constraint. */
    public static final Validator INDEX_CONSTRAINT_CONFLICTS = IndexConstraintConflictValidator.INSTANCE;

    /** Checks the numbers of properties and labels of indexes suit their type, and the labels are the node's. */
    public static final Validator INDEX_SHAPES = IndexShapeValidator.INSTANCE;

    /** Checks the types of indexed properties suit the type of the index. */
    public static final Validator INDEX_PROPERTY_TYPES = IndexPropertyTypeValidator.INSTANCE;

    /** Checks the dimensions of vector indexes match those of the indexed property. */
    public static final Validator VECTOR_DIMENSIONS = VectorDimensionValidator.INSTANCE;

    /** Checks the properties of {@code PROPERTY_TYPE} constraints have a type. */
    public static final Validator PROPERTY_TYPE_CONSTRAINTS = PropertyTypeConstraintValidator.INSTANCE;

    private ConsistentSchemaValidators() {}

    /**
     * Every consistent schema validator, in the order {@link org.neo4j.spec.graph.validate.Validators#consistentSchema()}
     * runs them.
     */
    public static List<Validator> all() {
        return List.of(
                SCHEMA_NAMES,
                UNIQUE_SCHEMA_NAMES,
                DUPLICATE_CONSTRAINTS,
                DUPLICATE_INDEXES,
                INDEX_CONSTRAINT_CONFLICTS,
                INDEX_SHAPES,
                INDEX_PROPERTY_TYPES,
                VECTOR_DIMENSIONS,
                PROPERTY_TYPE_CONSTRAINTS);
    }
}
