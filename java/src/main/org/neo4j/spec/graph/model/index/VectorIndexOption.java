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

import java.util.Objects;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/vector-indexes/#configuration-settings
 */
/** Configuration of a vector index. */
public final class VectorIndexOption implements IndexOption {
    private final Integer dimensions;

    private final String similarityFunction;

    private final Float defaultSearchExpansionFactor;

    private final boolean quantizationEnabled;

    private final String quantizationType;

    private final int hnswM;

    private final int hnswEfConstruction;

    private VectorIndexOption(Builder builder) {
        this.dimensions = builder.dimensions;
        this.similarityFunction = builder.similarityFunction;
        this.defaultSearchExpansionFactor = builder.defaultSearchExpansionFactor;
        this.quantizationEnabled = builder.quantizationEnabled;
        this.quantizationType = builder.quantizationType;
        this.hnswM = builder.hnswM;
        this.hnswEfConstruction = builder.hnswEfConstruction;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Number of dimensions of the indexed vectors. */
    public Integer getDimensions() {
        return this.dimensions;
    }

    /** Function used to compare vectors, e.g. cosine or euclidean. */
    public String getSimilarityFunction() {
        return this.similarityFunction;
    }

    /** Default expansion factor applied to searches of the index. */
    public Float getDefaultSearchExpansionFactor() {
        return this.defaultSearchExpansionFactor;
    }

    /** Whether vector quantization is enabled. */
    public boolean getQuantizationEnabled() {
        return this.quantizationEnabled;
    }

    /** Type of vector quantization used. */
    public String getQuantizationType() {
        return this.quantizationType;
    }

    /** Maximum number of connections per node in the HNSW graph. */
    public int getHnswM() {
        return this.hnswM;
    }

    /** Number of nearest neighbours considered when building the HNSW graph. */
    public int getHnswEfConstruction() {
        return this.hnswEfConstruction;
    }

    @Override
    public IndexType getType() {
        return IndexType.VECTOR;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VectorIndexOption)) return false;
        VectorIndexOption that = (VectorIndexOption) other;
        return Objects.equals(dimensions, that.dimensions)
                && Objects.equals(similarityFunction, that.similarityFunction)
                && Objects.equals(defaultSearchExpansionFactor, that.defaultSearchExpansionFactor)
                && quantizationEnabled == that.quantizationEnabled
                && Objects.equals(quantizationType, that.quantizationType)
                && hnswM == that.hnswM
                && hnswEfConstruction == that.hnswEfConstruction;
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
        return "VectorIndexOption(dimensions=" + dimensions + ", similarityFunction=" + similarityFunction
                + ", defaultSearchExpansionFactor=" + defaultSearchExpansionFactor + ", quantizationEnabled="
                + quantizationEnabled + ", quantizationType=" + quantizationType + ", hnswM=" + hnswM
                + ", hnswEfConstruction=" + hnswEfConstruction + ")";
    }

    public static final class Builder {
        private Integer dimensions = null;
        private String similarityFunction = "cosine";
        private Float defaultSearchExpansionFactor = null;
        private boolean quantizationEnabled = true;
        private String quantizationType = "scalar";
        private int hnswM = 16;
        private int hnswEfConstruction = 100;

        private Builder() {}

        public Builder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder similarityFunction(String similarityFunction) {
            this.similarityFunction = similarityFunction;
            return this;
        }

        public Builder defaultSearchExpansionFactor(Float defaultSearchExpansionFactor) {
            this.defaultSearchExpansionFactor = defaultSearchExpansionFactor;
            return this;
        }

        public Builder quantizationEnabled(boolean quantizationEnabled) {
            this.quantizationEnabled = quantizationEnabled;
            return this;
        }

        public Builder quantizationType(String quantizationType) {
            this.quantizationType = quantizationType;
            return this;
        }

        public Builder hnswM(int hnswM) {
            this.hnswM = hnswM;
            return this;
        }

        public Builder hnswEfConstruction(int hnswEfConstruction) {
            this.hnswEfConstruction = hnswEfConstruction;
            return this;
        }

        public VectorIndexOption build() {
            return new VectorIndexOption(this);
        }
    }
}
