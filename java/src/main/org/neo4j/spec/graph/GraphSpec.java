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

import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.migrate.DocumentVersion;
import org.neo4j.spec.graph.migrate.Migrations;
import org.neo4j.spec.graph.migrate.SpecVersion;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;
import org.neo4j.spec.graph.validate.ValidatorSet;

/**
 * Decodes, encodes and validates Neo4j Ontology Graph Specification models.
 *
 * <pre>{@code
 * GraphSpec spec = GraphSpec.defaults();
 * GraphModel model = spec.decode(yaml, Format.YAML);
 * List<Issue> issues = spec.validate(model, Validators.core());
 * String json = spec.encode(model, Format.JSON);
 * }</pre>
 *
 * <p>A {@link GraphModel} always represents the {@linkplain SpecVersion#LATEST latest} version of the specification
 * in longhand. Decoding migrates a document of any version that {@link Migrations} can reach up to it, and encoding
 * migrates it to the version asked for, so documents can be converted forwards and backwards.
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class GraphSpec {

    private static final GraphSpec DEFAULTS = builder().build();

    private final Migrations migrations;
    private final boolean failOnUnknownFields;

    private GraphSpec(Builder builder) {
        this.migrations = builder.migrations == null ? Migrations.defaults() : builder.migrations;
        this.failOnUnknownFields = builder.failOnUnknownFields;
    }

    /** A {@code GraphSpec} with the default settings and the built-in {@link Migrations#defaults() migrations}. */
    public static GraphSpec defaults() {
        return DEFAULTS;
    }

    /** A builder starting from the default settings. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder starting from this instance's settings. */
    public Builder toBuilder() {
        return new Builder().migrations(migrations).failOnUnknownFields(failOnUnknownFields);
    }

    /** The migrations used to convert documents between versions. */
    public Migrations migrations() {
        return migrations;
    }

    /** Whether decoding rejects fields the specification doesn't define, outside of {@code custom} data. */
    public boolean failOnUnknownFields() {
        return failOnUnknownFields;
    }

    /**
     * Decodes a document, detecting its version from its contents, such as a graph spec document's {@code schema}
     * URL. Shorthand is expanded.
     *
     * @throws GraphSpecException if the document can't be read or isn't a valid model
     */
    public GraphModel decode(String text, Format format) {
        return decode(text, format, null);
    }

    /**
     * Decodes a document of the given version, migrating it to the latest version.
     *
     * @param inputVersion the version of the document, or {@code null} to detect it from the contents
     * @throws GraphSpecException if the document can't be read, can't be migrated or isn't a valid model
     */
    public GraphModel decode(String text, Format format, DocumentVersion inputVersion) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(format, "format");
        throw new UnsupportedOperationException("TODO");
    }

    /** Decodes a document read from a {@link Reader}, detecting its version. The reader isn't closed. */
    public GraphModel decode(Reader reader, Format format) {
        return decode(reader, format, null);
    }

    /** Decodes a document of the given version read from a {@link Reader}. The reader isn't closed. */
    public GraphModel decode(Reader reader, Format format, DocumentVersion inputVersion) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(format, "format");
        throw new UnsupportedOperationException("TODO");
    }

    /** Encodes a model as a document of the {@linkplain SpecVersion#LATEST latest} version, in longhand. */
    public String encode(GraphModel model, Format format) {
        return encode(model, format, SpecVersion.LATEST);
    }

    /**
     * Encodes a model as a document of the given version, in longhand.
     *
     * @throws GraphSpecException if the model can't be migrated to that version
     */
    public String encode(GraphModel model, Format format, DocumentVersion outputVersion) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(outputVersion, "outputVersion");
        throw new UnsupportedOperationException("TODO");
    }

    /** Encodes a model to a {@link Writer} as a document of the given version. The writer isn't closed. */
    public void encode(GraphModel model, Format format, DocumentVersion outputVersion, Writer writer) {
        Objects.requireNonNull(writer, "writer");
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Validates a model, running each validator and its dependencies once.
     *
     * @param validators validators or sets of them, such as {@code Validators.core()}, in any combination
     * @return every issue found, empty when the model has none
     */
    public List<Issue> validate(GraphModel model, Validator... validators) {
        return validate(model, Arrays.asList(validators));
    }

    /** Validates a model against a collection of validators or sets of them. */
    public List<Issue> validate(GraphModel model, Collection<? extends Validator> validators) {
        Objects.requireNonNull(model, "model");
        return ValidatorSet.of(validators).validate(model);
    }

    /** Builds a {@link GraphSpec}. */
    public static final class Builder {

        private Migrations migrations;
        private boolean failOnUnknownFields = true;

        private Builder() {}

        /** The migrations used to convert documents between versions, {@link Migrations#defaults()} by default. */
        public Builder migrations(Migrations migrations) {
            this.migrations = Objects.requireNonNull(migrations, "migrations");
            return this;
        }

        /** Whether decoding rejects fields the specification doesn't define. {@code true} by default. */
        public Builder failOnUnknownFields(boolean failOnUnknownFields) {
            this.failOnUnknownFields = failOnUnknownFields;
            return this;
        }

        public GraphSpec build() {
            return new GraphSpec(this);
        }
    }
}
