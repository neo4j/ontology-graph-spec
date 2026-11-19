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
package org.neo4j.spec.graph.model;

import java.util.Optional;

/** A mapping from a data source table to the graph: a node, relationship or query mapping. */
public abstract class Mapping {
    Mapping() {}

    /** @return the kind of mapping */
    public abstract MappingType type();

    /** @return id of another mapping in the same list which must be imported before this one */
    public abstract Optional<String> dependsOn();

    /** @return optional id of this mapping, so other mappings in the same list can reference it */
    public abstract Optional<String> id();

    /** @return id of the source table */
    public abstract String table();
}
