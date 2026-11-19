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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A problem found in a model by a {@link Validator}. */
public final class Issue {

    private final String code;
    private final String message;
    private final String path;
    private final Map<String, String> details;

    private Issue(String code, String message, String path, Map<String, String> details) {
        this.code = requireText(code, "code");
        this.message = requireText(message, "message");
        this.path = path;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    /**
     * Creates an issue with no details.
     *
     * @param code a stable identifier for the kind of issue, such as {@code missing_relationship_to_node}
     * @param message a human-readable description
     * @param path where the issue is, as a dot-separated path such as {@code relationships.acted-in.to.film}, or
     *     {@code null}
     */
    public static Issue of(String code, String message, String path) {
        return new Issue(code, message, path, Map.of());
    }

    /** Creates an issue with extra details as string keys and values. */
    public static Issue of(String code, String message, String path, Map<String, String> details) {
        return new Issue(code, message, path, Objects.requireNonNull(details, "details"));
    }

    /** A stable identifier for the kind of issue, such as {@code missing_relationship_to_node}. */
    public String code() {
        return code;
    }

    /** A human-readable description. */
    public String message() {
        return message;
    }

    /** Where the issue is, as a dot-separated path such as {@code relationships.acted-in.to.film}. */
    public Optional<String> path() {
        return Optional.ofNullable(path);
    }

    /** Extra information, often empty. */
    public Map<String, String> details() {
        return details;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException("An issue's " + name + " can't be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Issue)) {
            return false;
        }
        Issue other = (Issue) o;
        return code.equals(other.code)
                && message.equals(other.message)
                && Objects.equals(path, other.path)
                && details.equals(other.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, path, details);
    }

    @Override
    public String toString() {
        return code + ": " + message + (path == null ? "" : " (" + path + ")")
                + (details.isEmpty() ? "" : " " + details);
    }
}
