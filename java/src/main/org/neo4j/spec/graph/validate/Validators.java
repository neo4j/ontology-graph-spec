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

import org.neo4j.spec.graph.validate.consistent.ConsistentSchemaValidators;
import org.neo4j.spec.graph.validate.core.CoreValidators;
import org.neo4j.spec.graph.validate.labelled.LabelledValidators;
import org.neo4j.spec.graph.validate.wellconstrained.WellConstrainedValidators;

/** The built-in sets of validators. Combine them with each other and your own using {@link ValidatorSet#and}. */
public final class Validators {

    private static final ValidatorSet CORE = ValidatorSet.of(CoreValidators.all());
    private static final ValidatorSet LABELLED = ValidatorSet.of(LabelledValidators.all());
    private static final ValidatorSet CONSISTENT_SCHEMA = ValidatorSet.of(ConsistentSchemaValidators.all());
    private static final ValidatorSet WELL_CONSTRAINED = ValidatorSet.of(WellConstrainedValidators.all());
    private static final ValidatorSet ALL =
            CORE.and(LABELLED).and(CONSISTENT_SCHEMA).and(WELL_CONSTRAINED);

    private Validators() {}

    /**
     * Checks a model is valid: that it follows the rules of the format, including a released graph spec
     * {@code schema} URL.
     */
    public static ValidatorSet core() {
        return CORE;
    }

    /**
     * Checks a model is complete enough to work with: every node has an identifier label, and every relationship has a
     * type and {@code from} and {@code to} nodes.
     */
    public static ValidatorSet labelled() {
        return LABELLED;
    }

    /**
     * Checks a model's properties, indexes and constraints can be created in Neo4j: names are set and unique,
     * constraints and indexes aren't duplicated, and definitions don't conflict. Its validators only run when the
     * model's references, constraint property counts and index options are valid according to the core set.
     */
    public static ValidatorSet consistentSchema() {
        return CONSISTENT_SCHEMA;
    }

    /**
     * Reviews a model's design against recommended practice: every node has properties and a constraint, and every
     * property has a type.
     */
    public static ValidatorSet wellConstrained() {
        return WELL_CONSTRAINED;
    }

    /** Every built-in check in this library: the core, labelled, consistent schema and well constrained sets. */
    public static ValidatorSet all() {
        return ALL;
    }
}
