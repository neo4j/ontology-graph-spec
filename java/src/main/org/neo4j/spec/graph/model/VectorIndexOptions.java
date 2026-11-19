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

import java.util.Objects;
import java.util.Optional;

/** Configuration of a vector index. */
public final class VectorIndexOptions extends IndexOptions {
    private final Integer dimensions;
    private final String similarityFunction;
    private final Double defaultSearchExpansionFactor;
    private final Boolean quantizationEnabled;
    private final String quantizationType;
    private final Integer hnswM;
    private final Integer hnswEfConstruction;

    private VectorIndexOptions(Builder b) {
        this.dimensions = b.dimensions;
        this.similarityFunction = b.similarityFunction;
        this.defaultSearchExpansionFactor = b.defaultSearchExpansionFactor;
        this.quantizationEnabled = b.quantizationEnabled;
        this.quantizationType = b.quantizationType;
        this.hnswM = b.hnswM;
        this.hnswEfConstruction = b.hnswEfConstruction;
    }

    /**
     * Creates an empty builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialised with the values of this instance.
     *
     * @return a new builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** @return number of dimensions of the indexed vectors (wire key {@code vector.dimensions}). */
    public Optional<Integer> dimensions() {
        return Optional.ofNullable(dimensions);
    }

    /** @return function used to compare vectors, e.g. cosine or euclidean (wire key {@code vector.similarity_function}). */
    public Optional<String> similarityFunction() {
        return Optional.ofNullable(similarityFunction);
    }

    /** @return default expansion factor applied to searches of the index (wire key {@code vector.default_search_expansion_factor}). */
    public Optional<Double> defaultSearchExpansionFactor() {
        return Optional.ofNullable(defaultSearchExpansionFactor);
    }

    /** @return whether vector quantization is enabled (wire key {@code vector.quantization.enabled}). */
    public Optional<Boolean> quantizationEnabled() {
        return Optional.ofNullable(quantizationEnabled);
    }

    /** @return type of vector quantization used (wire key {@code vector.quantization.type}). */
    public Optional<String> quantizationType() {
        return Optional.ofNullable(quantizationType);
    }

    /** @return maximum connections per node in the HNSW graph (wire key {@code vector.hnsw.m}). */
    public Optional<Integer> hnswM() {
        return Optional.ofNullable(hnswM);
    }

    /** @return nearest neighbours considered when building the HNSW graph (wire key {@code vector.hnsw.ef_construction}). */
    public Optional<Integer> hnswEfConstruction() {
        return Optional.ofNullable(hnswEfConstruction);
    }

    @Override
    public IndexType type() {
        return IndexType.VECTOR;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VectorIndexOptions that = (VectorIndexOptions) o;
        return Objects.equals(dimensions, that.dimensions)
                && Objects.equals(similarityFunction, that.similarityFunction)
                && Objects.equals(defaultSearchExpansionFactor, that.defaultSearchExpansionFactor)
                && Objects.equals(quantizationEnabled, that.quantizationEnabled)
                && Objects.equals(quantizationType, that.quantizationType)
                && Objects.equals(hnswM, that.hnswM)
                && Objects.equals(hnswEfConstruction, that.hnswEfConstruction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                dimensions,
                similarityFunction,
                defaultSearchExpansionFactor,
                quantizationEnabled,
                quantizationType,
                hnswM,
                hnswEfConstruction);
    }

    @Override
    public String toString() {
        return "VectorIndexOptions{" + "dimensions="
                + dimensions + ", similarityFunction="
                + similarityFunction + ", defaultSearchExpansionFactor="
                + defaultSearchExpansionFactor + ", quantizationEnabled="
                + quantizationEnabled + ", quantizationType="
                + quantizationType + ", hnswM="
                + hnswM + ", hnswEfConstruction="
                + hnswEfConstruction + "}";
    }

    /** Builder for {@link VectorIndexOptions}. */
    public static final class Builder {
        private Integer dimensions;
        private String similarityFunction;
        private Double defaultSearchExpansionFactor;
        private Boolean quantizationEnabled;
        private String quantizationType;
        private Integer hnswM;
        private Integer hnswEfConstruction;

        private Builder() {}

        private Builder(VectorIndexOptions v) {
            this.dimensions = v.dimensions;
            this.similarityFunction = v.similarityFunction;
            this.defaultSearchExpansionFactor = v.defaultSearchExpansionFactor;
            this.quantizationEnabled = v.quantizationEnabled;
            this.quantizationType = v.quantizationType;
            this.hnswM = v.hnswM;
            this.hnswEfConstruction = v.hnswEfConstruction;
        }

        /**
         * Sets number of dimensions of the indexed vectors (wire key {@code vector.dimensions}).
         *
         * @param dimensions the value, or null to clear it
         * @return this builder
         */
        public Builder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * Sets function used to compare vectors, e.g. cosine or euclidean (wire key {@code vector.similarity_function}).
         *
         * @param similarityFunction the value, or null to clear it
         * @return this builder
         */
        public Builder similarityFunction(String similarityFunction) {
            this.similarityFunction = similarityFunction;
            return this;
        }

        /**
         * Sets default expansion factor applied to searches of the index (wire key {@code vector.default_search_expansion_factor}).
         *
         * @param defaultSearchExpansionFactor the value, or null to clear it
         * @return this builder
         */
        public Builder defaultSearchExpansionFactor(Double defaultSearchExpansionFactor) {
            this.defaultSearchExpansionFactor = defaultSearchExpansionFactor;
            return this;
        }

        /**
         * Sets whether vector quantization is enabled (wire key {@code vector.quantization.enabled}).
         *
         * @param quantizationEnabled the value, or null to clear it
         * @return this builder
         */
        public Builder quantizationEnabled(Boolean quantizationEnabled) {
            this.quantizationEnabled = quantizationEnabled;
            return this;
        }

        /**
         * Sets type of vector quantization used (wire key {@code vector.quantization.type}).
         *
         * @param quantizationType the value, or null to clear it
         * @return this builder
         */
        public Builder quantizationType(String quantizationType) {
            this.quantizationType = quantizationType;
            return this;
        }

        /**
         * Sets maximum connections per node in the HNSW graph (wire key {@code vector.hnsw.m}).
         *
         * @param hnswM the value, or null to clear it
         * @return this builder
         */
        public Builder hnswM(Integer hnswM) {
            this.hnswM = hnswM;
            return this;
        }

        /**
         * Sets nearest neighbours considered when building the HNSW graph (wire key {@code vector.hnsw.ef_construction}).
         *
         * @param hnswEfConstruction the value, or null to clear it
         * @return this builder
         */
        public Builder hnswEfConstruction(Integer hnswEfConstruction) {
            this.hnswEfConstruction = hnswEfConstruction;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public VectorIndexOptions build() {
            return new VectorIndexOptions(this);
        }
    }
}
