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

/** The built-in sets of validators. Combine them with each other and your own using {@link ValidatorSet#and}. */
public final class Validators {

    private Validators() {}

    /**
     * Checks a model is valid: that it follows the rules of the format, including a released graph spec
     * {@code schema} URL.
     */
    public static ValidatorSet core() {
        throw new UnsupportedOperationException("TODO");
    }
}
