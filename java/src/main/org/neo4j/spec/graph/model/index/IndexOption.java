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
package org.neo4j.spec.graph.model.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.neo4j.spec.graph.model.type.IndexType;

/** Configuration specific to the type of an index. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FullTextIndexOption.class, name = "FULLTEXT"),
    @JsonSubTypes.Type(value = PointIndexOption.class, name = "POINT"),
    @JsonSubTypes.Type(value = VectorIndexOption.class, name = "VECTOR")
})
public interface IndexOption {
    /** The index type, written as the {@code type} discriminator rather than as a property. */
    @JsonIgnore
    IndexType getType();
}
