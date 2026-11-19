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

/**
 * Conversion of documents between versions, and between document types.
 *
 * <p>A {@link org.neo4j.spec.graph.migrate.Migration} is one step between two
 * {@link org.neo4j.spec.graph.migrate.DocumentVersion}s (a {@link org.neo4j.spec.graph.migrate.DocumentType} plus a
 * semantic version). {@link org.neo4j.spec.graph.migrate.Migrations} holds steps and
 * {@link org.neo4j.spec.graph.migrate.VersionDetector}s, and chains steps along the shortest path to reach any
 * connected version, forwards or backwards.
 *
 * <h2>Adding a step for a new spec version</h2>
 *
 * <ol>
 *   <li>Add the version to {@link org.neo4j.spec.graph.migrate.SpecVersion} and make it {@code LATEST}.
 *   <li>Add a step from the previous version and one back, by extending
 *       {@link org.neo4j.spec.graph.migrate.TreeMigration} and implementing only the tree transform.
 *   <li>Register both in {@link org.neo4j.spec.graph.migrate.Migrations#defaults()}.
 * </ol>
 *
 * <h2>Adding a new document type</h2>
 *
 * <ol>
 *   <li>Create a type with {@link org.neo4j.spec.graph.migrate.DocumentType#of(String)}.
 *   <li>Implement a {@link org.neo4j.spec.graph.migrate.VersionDetector} that recognises its documents.
 *   <li>Register steps from the new type to a graph spec version, and back, with
 *       {@link org.neo4j.spec.graph.migrate.Migrations.Builder#add}. Decoding then migrates such documents to the
 *       latest graph spec, and encoding can write them out again.
 * </ol>
 */
package org.neo4j.spec.graph.migrate;
