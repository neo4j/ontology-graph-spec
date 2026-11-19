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
package org.neo4j.spec.graph.migrate;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * One step that converts a document from one {@link DocumentVersion} to another, such as graph spec 1.0.0 to 1.1.0,
 * or 1.1.0 back to 1.0.0. Steps work on the document tree and are chained by {@link Migrations} to reach any version
 * they connect to.
 */
public interface Migration {

    /** The version of the documents this step reads. */
    DocumentVersion from();

    /** The version of the documents this step writes. */
    DocumentVersion to();

    /**
     * Converts a document. The input may be modified and returned, or a new tree returned.
     *
     * @param document a document of version {@link #from()}
     * @return the document as version {@link #to()}
     */
    ObjectNode apply(ObjectNode document);
}
