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

import java.util.Set;
import org.neo4j.spec.graph.validate.Validator;
import org.neo4j.spec.graph.validate.core.CoreValidators;

/** What every consistent schema validator depends on. */
final class Dependencies {

    private Dependencies() {}

    /** The core validators that must find no issues first, so ids resolve and constraints and options are well formed. */
    static Set<Validator> core() {
        return Set.of(
                CoreValidators.REFERENCES, CoreValidators.CONSTRAINT_PROPERTY_COUNTS, CoreValidators.INDEX_OPTIONS);
    }
}
