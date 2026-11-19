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
package org.neo4j.spec.graph.validate.wellconstrained;

import java.util.List;
import org.neo4j.spec.graph.validate.Validator;

/**
 * The validators that make up {@link org.neo4j.spec.graph.validate.Validators#wellConstrained()}, to pick individually
 * or combine with {@link org.neo4j.spec.graph.validate.ValidatorSet#of(Validator...)}. They don't depend on each other.
 */
public final class WellConstrainedValidators {

    /** Checks every node has properties. */
    public static final Validator NODE_PROPERTIES = NodePropertiesValidator.INSTANCE;

    /** Checks every node has a constraint. */
    public static final Validator NODE_CONSTRAINTS = NodeConstraintsValidator.INSTANCE;

    /** Checks every property has a type. */
    public static final Validator PROPERTY_TYPES = PropertyTypeValidator.INSTANCE;

    private WellConstrainedValidators() {}

    /**
     * Every well constrained validator, in the order {@link org.neo4j.spec.graph.validate.Validators#wellConstrained()}
     * runs them.
     */
    public static List<Validator> all() {
        return List.of(NODE_PROPERTIES, NODE_CONSTRAINTS, PROPERTY_TYPES);
    }
}
