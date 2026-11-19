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
package org.neo4j.spec.graph.validate.core;

import java.util.List;
import org.neo4j.spec.graph.validate.Validator;

/**
 * The validators that make up {@link org.neo4j.spec.graph.validate.Validators#core()}, to pick individually or combine
 * with {@link org.neo4j.spec.graph.validate.ValidatorSet#of(Validator...)}. They don't depend on each other.
 */
public final class CoreValidators {

    /** Checks the {@code schema} is the URL of a released version of the specification. */
    public static final Validator SCHEMA_URL = SchemaUrlValidator.INSTANCE;

    /** Checks references to nodes and properties resolve. */
    public static final Validator REFERENCES = ReferenceValidator.INSTANCE;

    /** Checks {@code EXISTS} and {@code PROPERTY_TYPE} constraints cover exactly one property. */
    public static final Validator CONSTRAINT_PROPERTY_COUNTS = ConstraintPropertyCountValidator.INSTANCE;

    /** Checks index options match the index type. */
    public static final Validator INDEX_OPTIONS = IndexOptionsValidator.INSTANCE;

    /** Checks properties don't set both {@code pattern} and {@code one_of}. */
    public static final Validator PROPERTY_PATTERNS = PropertyPatternValidator.INSTANCE;

    /** Checks the counts of relationship ends. */
    public static final Validator RELATIONSHIP_COUNTS = RelationshipCountValidator.INSTANCE;

    /** Checks lists don't repeat an entry. */
    public static final Validator REPEATED_ENTRIES = RepeatedEntryValidator.INSTANCE;

    /** Checks names and descriptions aren't too long. */
    public static final Validator LENGTHS = LengthValidator.INSTANCE;

    private CoreValidators() {}

    /** Every core validator, in the order {@link org.neo4j.spec.graph.validate.Validators#core()} runs them. */
    public static List<Validator> all() {
        return List.of(
                SCHEMA_URL,
                REFERENCES,
                CONSTRAINT_PROPERTY_COUNTS,
                INDEX_OPTIONS,
                PROPERTY_PATTERNS,
                RELATIONSHIP_COUNTS,
                REPEATED_ENTRIES,
                LENGTHS);
    }
}
