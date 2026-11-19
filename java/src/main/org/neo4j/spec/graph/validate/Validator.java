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
import java.util.Set;
import org.neo4j.spec.graph.model.GraphModel;

/**
 * A check of a model, which returns the issues it finds. Implement this to add your own checks, and combine them with
 * the built-in ones using {@link ValidatorSet}.
 *
 * <p>A validator can {@linkplain #dependsOn() depend on} others: it then only runs when they found no issues, which
 * avoids a cascade of issues from a single mistake. Dependencies run even if they weren't asked for.
 */
@FunctionalInterface
public interface Validator {

    /**
     * Checks a model.
     *
     * @return the issues found, empty when there are none
     */
    List<Issue> validate(GraphModel model);

    /** The validators that must find no issues for this one to run. Empty by default. */
    default Set<Validator> dependsOn() {
        return Set.of();
    }
}
