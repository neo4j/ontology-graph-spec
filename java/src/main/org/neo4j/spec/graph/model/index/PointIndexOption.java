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
// Generated from org/neo4j/spec/graph/model/index/PointIndexOption.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.index;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * https://neo4j.com/docs/cypher-manual/current/indexes/search-performance-indexes/create-indexes/#point-indexes-examples
 */
@JsonClassDescription("Configuration of a point index.")
@JsonTypeName("POINT")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class PointIndexOption implements IndexOption {
    private final List<Double> cartesianMin;

    private final List<Double> cartesianMax;

    private final List<Double> cartesian3DMin;

    private final List<Double> cartesian3DMax;

    private final List<Double> wgs84Min;

    private final List<Double> wgs84Max;

    private final List<Double> wgs843DMin;

    private final List<Double> wgs843DMax;

    public PointIndexOption(
            List<Double> cartesianMin,
            List<Double> cartesianMax,
            List<Double> cartesian3DMin,
            List<Double> cartesian3DMax,
            List<Double> wgs84Min,
            List<Double> wgs84Max,
            List<Double> wgs843DMin,
            List<Double> wgs843DMax) {
        this.cartesianMin = cartesianMin;
        this.cartesianMax = cartesianMax;
        this.cartesian3DMin = cartesian3DMin;
        this.cartesian3DMax = cartesian3DMax;
        this.wgs84Min = wgs84Min;
        this.wgs84Max = wgs84Max;
        this.wgs843DMin = wgs843DMin;
        this.wgs843DMax = wgs843DMax;
    }

    public PointIndexOption(
            List<Double> cartesianMin,
            List<Double> cartesianMax,
            List<Double> cartesian3DMin,
            List<Double> cartesian3DMax,
            List<Double> wgs84Min,
            List<Double> wgs84Max,
            List<Double> wgs843DMin) {
        this(
                cartesianMin,
                cartesianMax,
                cartesian3DMin,
                cartesian3DMax,
                wgs84Min,
                wgs84Max,
                wgs843DMin,
                List.of(180.0, 90.0, 1000000.0));
    }

    public PointIndexOption(
            List<Double> cartesianMin,
            List<Double> cartesianMax,
            List<Double> cartesian3DMin,
            List<Double> cartesian3DMax,
            List<Double> wgs84Min,
            List<Double> wgs84Max) {
        this(
                cartesianMin,
                cartesianMax,
                cartesian3DMin,
                cartesian3DMax,
                wgs84Min,
                wgs84Max,
                List.of(-180.0, -90.0, -1000000.0),
                List.of(180.0, 90.0, 1000000.0));
    }

    public PointIndexOption(
            List<Double> cartesianMin,
            List<Double> cartesianMax,
            List<Double> cartesian3DMin,
            List<Double> cartesian3DMax,
            List<Double> wgs84Min) {
        this(
                cartesianMin,
                cartesianMax,
                cartesian3DMin,
                cartesian3DMax,
                wgs84Min,
                List.of(180.0, 90.0),
                List.of(-180.0, -90.0, -1000000.0),
                List.of(180.0, 90.0, 1000000.0));
    }

    public PointIndexOption(
            List<Double> cartesianMin,
            List<Double> cartesianMax,
            List<Double> cartesian3DMin,
            List<Double> cartesian3DMax) {
        this(
                cartesianMin,
                cartesianMax,
                cartesian3DMin,
                cartesian3DMax,
                List.of(-180.0, -90.0),
                List.of(180.0, 90.0),
                List.of(-180.0, -90.0, -1000000.0),
                List.of(180.0, 90.0, 1000000.0));
    }

    public PointIndexOption(List<Double> cartesianMin, List<Double> cartesianMax, List<Double> cartesian3DMin) {
        this(
                cartesianMin,
                cartesianMax,
                cartesian3DMin,
                List.of(1000000.0, 1000000.0, 1000000.0),
                List.of(-180.0, -90.0),
                List.of(180.0, 90.0),
                List.of(-180.0, -90.0, -1000000.0),
                List.of(180.0, 90.0, 1000000.0));
    }

    public PointIndexOption(List<Double> cartesianMin, List<Double> cartesianMax) {
        this(
                cartesianMin,
                cartesianMax,
                List.of(-1000000.0, -1000000.0, -1000000.0),
                List.of(1000000.0, 1000000.0, 1000000.0),
                List.of(-180.0, -90.0),
                List.of(180.0, 90.0),
                List.of(-180.0, -90.0, -1000000.0),
                List.of(180.0, 90.0, 1000000.0));
    }

    public PointIndexOption(List<Double> cartesianMin) {
        this(
                cartesianMin,
                List.of(1000000.0, 1000000.0),
                List.of(-1000000.0, -1000000.0, -1000000.0),
                List.of(1000000.0, 1000000.0, 1000000.0),
                List.of(-180.0, -90.0),
                List.of(180.0, 90.0),
                List.of(-180.0, -90.0, -1000000.0),
                List.of(180.0, 90.0, 1000000.0));
    }

    public PointIndexOption() {
        this(
                List.of(-1000000.0, -1000000.0),
                List.of(1000000.0, 1000000.0),
                List.of(-1000000.0, -1000000.0, -1000000.0),
                List.of(1000000.0, 1000000.0, 1000000.0),
                List.of(-180.0, -90.0),
                List.of(180.0, 90.0),
                List.of(-180.0, -90.0, -1000000.0),
                List.of(180.0, 90.0, 1000000.0));
    }

    public List<Double> getCartesianMin() {
        return this.cartesianMin;
    }

    public List<Double> getCartesianMax() {
        return this.cartesianMax;
    }

    public List<Double> getCartesian3DMin() {
        return this.cartesian3DMin;
    }

    public List<Double> getCartesian3DMax() {
        return this.cartesian3DMax;
    }

    public List<Double> getWgs84Min() {
        return this.wgs84Min;
    }

    public List<Double> getWgs84Max() {
        return this.wgs84Max;
    }

    public List<Double> getWgs843DMin() {
        return this.wgs843DMin;
    }

    public List<Double> getWgs843DMax() {
        return this.wgs843DMax;
    }

    @Override
    public IndexType getType() {
        return IndexType.POINT;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PointIndexOption)) return false;
        PointIndexOption that = (PointIndexOption) other;
        return Objects.equals(cartesianMin, that.cartesianMin)
                && Objects.equals(cartesianMax, that.cartesianMax)
                && Objects.equals(cartesian3DMin, that.cartesian3DMin)
                && Objects.equals(cartesian3DMax, that.cartesian3DMax)
                && Objects.equals(wgs84Min, that.wgs84Min)
                && Objects.equals(wgs84Max, that.wgs84Max)
                && Objects.equals(wgs843DMin, that.wgs843DMin)
                && Objects.equals(wgs843DMax, that.wgs843DMax);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(cartesianMin);
        result = 31 * result + Objects.hashCode(cartesianMax);
        result = 31 * result + Objects.hashCode(cartesian3DMin);
        result = 31 * result + Objects.hashCode(cartesian3DMax);
        result = 31 * result + Objects.hashCode(wgs84Min);
        result = 31 * result + Objects.hashCode(wgs84Max);
        result = 31 * result + Objects.hashCode(wgs843DMin);
        result = 31 * result + Objects.hashCode(wgs843DMax);
        return result;
    }

    @Override
    public String toString() {
        return "PointIndexOption(cartesianMin=" + cartesianMin + ", cartesianMax=" + cartesianMax + ", cartesian3DMin="
                + cartesian3DMin + ", cartesian3DMax=" + cartesian3DMax + ", wgs84Min=" + wgs84Min + ", wgs84Max="
                + wgs84Max + ", wgs843DMin=" + wgs843DMin + ", wgs843DMax=" + wgs843DMax + ")";
    }

    @JsonProperty("spatial.cartesian.min")
    @JsonPropertyDescription("Lower bounds of the 2D cartesian space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonCartesianMin() {
        return Objects.equals(this.cartesianMin, List.of(-1000000.0, -1000000.0)) ? null : this.cartesianMin;
    }

    @JsonProperty("spatial.cartesian.max")
    @JsonPropertyDescription("Upper bounds of the 2D cartesian space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonCartesianMax() {
        return Objects.equals(this.cartesianMax, List.of(1000000.0, 1000000.0)) ? null : this.cartesianMax;
    }

    @JsonProperty("spatial.cartesian-3d.min")
    @JsonPropertyDescription("Lower bounds of the 3D cartesian space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonCartesian3DMin() {
        return Objects.equals(this.cartesian3DMin, List.of(-1000000.0, -1000000.0, -1000000.0))
                ? null
                : this.cartesian3DMin;
    }

    @JsonProperty("spatial.cartesian-3d.max")
    @JsonPropertyDescription("Upper bounds of the 3D cartesian space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonCartesian3DMax() {
        return Objects.equals(this.cartesian3DMax, List.of(1000000.0, 1000000.0, 1000000.0))
                ? null
                : this.cartesian3DMax;
    }

    @JsonProperty("spatial.wgs-84.min")
    @JsonPropertyDescription("Lower bounds of the 2D WGS-84 space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonWgs84Min() {
        return Objects.equals(this.wgs84Min, List.of(-180.0, -90.0)) ? null : this.wgs84Min;
    }

    @JsonProperty("spatial.wgs-84.max")
    @JsonPropertyDescription("Upper bounds of the 2D WGS-84 space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonWgs84Max() {
        return Objects.equals(this.wgs84Max, List.of(180.0, 90.0)) ? null : this.wgs84Max;
    }

    @JsonProperty("spatial.wgs-84-3d.min")
    @JsonPropertyDescription("Lower bounds of the 3D WGS-84 space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonWgs843DMin() {
        return Objects.equals(this.wgs843DMin, List.of(-180.0, -90.0, -1000000.0)) ? null : this.wgs843DMin;
    }

    @JsonProperty("spatial.wgs-84-3d.max")
    @JsonPropertyDescription("Upper bounds of the 3D WGS-84 space.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Double> jsonWgs843DMax() {
        return Objects.equals(this.wgs843DMax, List.of(180.0, 90.0, 1000000.0)) ? null : this.wgs843DMax;
    }

    @JsonCreator
    private static PointIndexOption fromJson(
            @JsonProperty("spatial.cartesian.min") List<Double> cartesianMin,
            @JsonProperty("spatial.cartesian.max") List<Double> cartesianMax,
            @JsonProperty("spatial.cartesian-3d.min") List<Double> cartesian3DMin,
            @JsonProperty("spatial.cartesian-3d.max") List<Double> cartesian3DMax,
            @JsonProperty("spatial.wgs-84.min") List<Double> wgs84Min,
            @JsonProperty("spatial.wgs-84.max") List<Double> wgs84Max,
            @JsonProperty("spatial.wgs-84-3d.min") List<Double> wgs843DMin,
            @JsonProperty("spatial.wgs-84-3d.max") List<Double> wgs843DMax) {
        return new PointIndexOption(
                cartesianMin != null ? cartesianMin : List.of(-1000000.0, -1000000.0),
                cartesianMax != null ? cartesianMax : List.of(1000000.0, 1000000.0),
                cartesian3DMin != null ? cartesian3DMin : List.of(-1000000.0, -1000000.0, -1000000.0),
                cartesian3DMax != null ? cartesian3DMax : List.of(1000000.0, 1000000.0, 1000000.0),
                wgs84Min != null ? wgs84Min : List.of(-180.0, -90.0),
                wgs84Max != null ? wgs84Max : List.of(180.0, 90.0),
                wgs843DMin != null ? wgs843DMin : List.of(-180.0, -90.0, -1000000.0),
                wgs843DMax != null ? wgs843DMax : List.of(180.0, 90.0, 1000000.0));
    }
}
