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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.neo4j.spec.graph.GraphSpecException;

/**
 * An immutable set of {@link Migration} steps and {@link VersionDetector}s, which converts documents between any two
 * versions the steps connect, forwards or backwards and across document types.
 */
public final class Migrations {

    private static final Migrations DEFAULTS = builder()
            .detector(GraphSpecMigrations.detector())
            .add(GraphSpecMigrations.v1_0_0ToV1_1_0())
            .add(GraphSpecMigrations.v1_1_0ToV1_0_0())
            .build();

    private final List<Migration> steps;
    private final List<VersionDetector> detectors;

    private Migrations(Builder builder) {
        this.steps = Collections.unmodifiableList(new ArrayList<>(builder.steps.values()));
        this.detectors = Collections.unmodifiableList(new ArrayList<>(builder.detectors));
    }

    /** The built-in steps between the released graph spec versions, and the graph spec version detector. */
    public static Migrations defaults() {
        return DEFAULTS;
    }

    /** A builder starting empty. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder starting with these steps and detectors, to add more to. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.detectors.addAll(detectors);
        for (Migration step : steps) {
            builder.add(step);
        }
        return builder;
    }

    /** The registered steps, in registration order. */
    public List<Migration> migrations() {
        return steps;
    }

    /**
     * The version of a document, from the first detector (in registration order) that recognises it.
     *
     * @throws org.neo4j.spec.graph.GraphSpecException if no detector recognises it
     */
    public DocumentVersion detect(ObjectNode document) {
        Objects.requireNonNull(document, "document");
        for (VersionDetector detector : detectors) {
            Optional<DocumentVersion> version = detector.detect(document);
            if (version.isPresent()) {
                return version.get();
            }
        }
        throw new GraphSpecException("Unable to detect the version of the document");
    }

    /**
     * The shortest chain of steps from one version to another, empty when they're the same. Ties are broken by
     * registration order.
     *
     * @throws org.neo4j.spec.graph.GraphSpecException if no chain connects them
     */
    public List<Migration> path(DocumentVersion from, DocumentVersion to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.equals(to)) {
            return Collections.emptyList();
        }
        Map<DocumentVersion, Migration> via = new HashMap<>();
        Deque<DocumentVersion> queue = new ArrayDeque<>();
        queue.add(from);
        boolean found = false;
        while (!queue.isEmpty() && !found) {
            DocumentVersion current = queue.poll();
            for (Migration step : steps) {
                DocumentVersion next = step.to();
                if (step.from().equals(current) && !next.equals(from) && !via.containsKey(next)) {
                    via.put(next, step);
                    if (next.equals(to)) {
                        found = true;
                        break;
                    }
                    queue.add(next);
                }
            }
        }
        if (!found) {
            throw new GraphSpecException("No migration path from " + from + " to " + to);
        }
        List<Migration> path = new ArrayList<>();
        DocumentVersion at = to;
        while (!at.equals(from)) {
            Migration step = via.get(at);
            path.add(0, step);
            at = step.from();
        }
        return Collections.unmodifiableList(path);
    }

    /**
     * Converts a document from one version to another along {@link #path}. The given document is never modified.
     *
     * @throws org.neo4j.spec.graph.GraphSpecException if no chain connects them
     */
    public ObjectNode migrate(ObjectNode document, DocumentVersion from, DocumentVersion to) {
        Objects.requireNonNull(document, "document");
        List<Migration> path = path(from, to);
        ObjectNode result = document.deepCopy();
        for (Migration step : path) {
            result = step.apply(result);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Migrations" + steps;
    }

    /** Builds {@link Migrations}. */
    public static final class Builder {

        private final Map<String, Migration> steps = new LinkedHashMap<>();
        private final List<VersionDetector> detectors = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a step. A step with the same {@code from} and {@code to} as an earlier one replaces it (keeping its
         * position), which lets built-in steps be overridden.
         */
        public Builder add(Migration migration) {
            Objects.requireNonNull(migration, "migration");
            steps.put(migration.from() + " -> " + migration.to(), migration);
            return this;
        }

        /** Adds a detector, consulted after those added before it. */
        public Builder detector(VersionDetector detector) {
            detectors.add(Objects.requireNonNull(detector, "detector"));
            return this;
        }

        /** Builds the immutable set. */
        public Migrations build() {
            return new Migrations(this);
        }
    }
}
