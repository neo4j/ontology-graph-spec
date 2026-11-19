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
package org.neo4j.spec.graph.validate.labelled;

import java.util.List;
import org.neo4j.spec.graph.validate.Validator;

/**
 * The validators that make up {@link org.neo4j.spec.graph.validate.Validators#labelled()}, to pick individually or
 * combine with {@link org.neo4j.spec.graph.validate.ValidatorSet#of(Validator...)}. They don't depend on each other.
 */
public final class LabelledValidators {

    /** Checks every node has an identifier label. */
    public static final Validator NODE_IDENTIFIER_LABELS = NodeIdentifierLabelValidator.INSTANCE;

    /** Checks every relationship has a type. */
    public static final Validator RELATIONSHIP_TYPES = RelationshipTypeValidator.INSTANCE;

    /** Checks every relationship has {@code from} and {@code to} nodes. */
    public static final Validator RELATIONSHIP_ENDS = RelationshipEndsValidator.INSTANCE;

    private LabelledValidators() {}

    /** Every labelled validator, in the order {@link org.neo4j.spec.graph.validate.Validators#labelled()} runs them. */
    public static List<Validator> all() {
        return List.of(NODE_IDENTIFIER_LABELS, RELATIONSHIP_TYPES, RELATIONSHIP_ENDS);
    }
}
