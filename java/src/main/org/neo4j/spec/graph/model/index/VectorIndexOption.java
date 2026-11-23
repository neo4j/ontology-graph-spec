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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/vector-indexes/#configuration-settings
 */
@JsonClassDescription("Configuration of a vector index.")
@JsonTypeName("VECTOR")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class VectorIndexOption implements IndexOption {
    private final Integer dimensions;

    private final String similarityFunction;

    private final Float defaultSearchExpansionFactor;

    private final boolean quantizationEnabled;

    private final String quantizationType;

    private final int hnswM;

    private final int hnswEfConstruction;

    private VectorIndexOption(
            Integer dimensions,
            String similarityFunction,
            Float defaultSearchExpansionFactor,
            boolean quantizationEnabled,
            String quantizationType,
            int hnswM,
            int hnswEfConstruction) {
        this.dimensions = dimensions;
        this.similarityFunction = similarityFunction;
        this.defaultSearchExpansionFactor = defaultSearchExpansionFactor;
        this.quantizationEnabled = quantizationEnabled;
        this.quantizationType = quantizationType;
        this.hnswM = hnswM;
        this.hnswEfConstruction = hnswEfConstruction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getDimensions() {
        return this.dimensions;
    }

    public String getSimilarityFunction() {
        return this.similarityFunction;
    }

    public Float getDefaultSearchExpansionFactor() {
        return this.defaultSearchExpansionFactor;
    }

    public boolean getQuantizationEnabled() {
        return this.quantizationEnabled;
    }

    public String getQuantizationType() {
        return this.quantizationType;
    }

    public int getHnswM() {
        return this.hnswM;
    }

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
        int result = Objects.hashCode(dimensions);
        result = 31 * result + Objects.hashCode(similarityFunction);
        result = 31 * result + Objects.hashCode(defaultSearchExpansionFactor);
        result = 31 * result + Boolean.hashCode(quantizationEnabled);
        result = 31 * result + Objects.hashCode(quantizationType);
        result = 31 * result + Integer.hashCode(hnswM);
        result = 31 * result + Integer.hashCode(hnswEfConstruction);
        return result;
    }

    @Override
    public String toString() {
        return "VectorIndexOption(dimensions=" + dimensions + ", similarityFunction=" + similarityFunction
                + ", defaultSearchExpansionFactor=" + defaultSearchExpansionFactor + ", quantizationEnabled="
                + quantizationEnabled + ", quantizationType=" + quantizationType + ", hnswM=" + hnswM
                + ", hnswEfConstruction=" + hnswEfConstruction + ")";
    }

    @JsonProperty("vector.dimensions")
    @JsonPropertyDescription("Number of dimensions of the indexed vectors.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonDimensions() {
        return Objects.equals(this.dimensions, null) ? null : this.dimensions;
    }

    @JsonProperty("vector.similarity_function")
    @JsonPropertyDescription("Function used to compare vectors, e.g. cosine or euclidean.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonSimilarityFunction() {
        return Objects.equals(this.similarityFunction, "cosine") ? null : this.similarityFunction;
    }

    @JsonProperty("vector.default_search_expansion_factor")
    @JsonPropertyDescription("Default expansion factor applied to searches of the index.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Float jsonDefaultSearchExpansionFactor() {
        return Objects.equals(this.defaultSearchExpansionFactor, null) ? null : this.defaultSearchExpansionFactor;
    }

    @JsonProperty("vector.quantization.enabled")
    @JsonPropertyDescription("Whether vector quantization is enabled.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean jsonQuantizationEnabled() {
        return this.quantizationEnabled ? null : this.quantizationEnabled;
    }

    @JsonProperty("vector.quantization.type")
    @JsonPropertyDescription("Type of vector quantization used.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonQuantizationType() {
        return Objects.equals(this.quantizationType, "scalar") ? null : this.quantizationType;
    }

    @JsonProperty("vector.hnsw.m")
    @JsonPropertyDescription("Maximum number of connections per node in the HNSW graph.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonHnswM() {
        return this.hnswM == 16 ? null : this.hnswM;
    }

    @JsonProperty("vector.hnsw.ef_construction")
    @JsonPropertyDescription("Number of nearest neighbours considered when building the HNSW graph.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonHnswEfConstruction() {
        return this.hnswEfConstruction == 100 ? null : this.hnswEfConstruction;
    }

    @JsonCreator
    private static VectorIndexOption fromJson(
            @JsonProperty("vector.dimensions") Integer dimensions,
            @JsonProperty("vector.similarity_function") String similarityFunction,
            @JsonProperty("vector.default_search_expansion_factor") Float defaultSearchExpansionFactor,
            @JsonProperty("vector.quantization.enabled") Boolean quantizationEnabled,
            @JsonProperty("vector.quantization.type") String quantizationType,
            @JsonProperty("vector.hnsw.m") Integer hnswM,
            @JsonProperty("vector.hnsw.ef_construction") Integer hnswEfConstruction) {
        return new VectorIndexOption(
                dimensions,
                similarityFunction != null ? similarityFunction : "cosine",
                defaultSearchExpansionFactor,
                quantizationEnabled != null ? quantizationEnabled : true,
                quantizationType != null ? quantizationType : "scalar",
                hnswM != null ? hnswM : 16,
                hnswEfConstruction != null ? hnswEfConstruction : 100);
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
            return new VectorIndexOption(
                    dimensions,
                    similarityFunction,
                    defaultSearchExpansionFactor,
                    quantizationEnabled,
                    quantizationType,
                    hnswM,
                    hnswEfConstruction);
        }
    }
}
