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
package org.neo4j.spec.graph;

import java.util.Optional;

/** Thrown when a document can't be decoded, encoded or migrated. */
public class GraphSpecException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String path;

    public GraphSpecException(String message) {
        this(message, null, null);
    }

    public GraphSpecException(String message, Throwable cause) {
        this(message, null, cause);
    }

    /**
     * @param message a human-readable description of the problem
     * @param path the dot-separated document path the problem was found at, such as {@code nodes.person.labels}, or
     *     {@code null} when it doesn't apply to one place
     * @param cause the underlying cause, or {@code null}
     */
    public GraphSpecException(String message, String path, Throwable cause) {
        super(path == null ? message : message + " (at " + path + ")", cause);
        this.path = path;
    }

    /** The dot-separated document path the problem was found at, if it applies to one place. */
    public Optional<String> path() {
        return Optional.ofNullable(path);
    }
}
