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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Configuration of a point index. */
public final class PointIndexOptions extends IndexOptions {
    private final List<Double> cartesianMin;
    private final List<Double> cartesianMax;
    private final List<Double> cartesian3dMin;
    private final List<Double> cartesian3dMax;
    private final List<Double> wgs84Min;
    private final List<Double> wgs84Max;
    private final List<Double> wgs843dMin;
    private final List<Double> wgs843dMax;

    private PointIndexOptions(Builder b) {
        this.cartesianMin = Collections.unmodifiableList(new ArrayList<>(b.cartesianMin));
        this.cartesianMax = Collections.unmodifiableList(new ArrayList<>(b.cartesianMax));
        this.cartesian3dMin = Collections.unmodifiableList(new ArrayList<>(b.cartesian3dMin));
        this.cartesian3dMax = Collections.unmodifiableList(new ArrayList<>(b.cartesian3dMax));
        this.wgs84Min = Collections.unmodifiableList(new ArrayList<>(b.wgs84Min));
        this.wgs84Max = Collections.unmodifiableList(new ArrayList<>(b.wgs84Max));
        this.wgs843dMin = Collections.unmodifiableList(new ArrayList<>(b.wgs843dMin));
        this.wgs843dMax = Collections.unmodifiableList(new ArrayList<>(b.wgs843dMax));
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

    /** @return lower bounds of the 2D cartesian space (wire key {@code spatial.cartesian.min}). */
    public List<Double> cartesianMin() {
        return cartesianMin;
    }

    /** @return upper bounds of the 2D cartesian space (wire key {@code spatial.cartesian.max}). */
    public List<Double> cartesianMax() {
        return cartesianMax;
    }

    /** @return lower bounds of the 3D cartesian space (wire key {@code spatial.cartesian-3d.min}). */
    public List<Double> cartesian3dMin() {
        return cartesian3dMin;
    }

    /** @return upper bounds of the 3D cartesian space (wire key {@code spatial.cartesian-3d.max}). */
    public List<Double> cartesian3dMax() {
        return cartesian3dMax;
    }

    /** @return lower bounds of the 2D WGS-84 space (wire key {@code spatial.wgs-84.min}). */
    public List<Double> wgs84Min() {
        return wgs84Min;
    }

    /** @return upper bounds of the 2D WGS-84 space (wire key {@code spatial.wgs-84.max}). */
    public List<Double> wgs84Max() {
        return wgs84Max;
    }

    /** @return lower bounds of the 3D WGS-84 space (wire key {@code spatial.wgs-84-3d.min}). */
    public List<Double> wgs843dMin() {
        return wgs843dMin;
    }

    /** @return upper bounds of the 3D WGS-84 space (wire key {@code spatial.wgs-84-3d.max}). */
    public List<Double> wgs843dMax() {
        return wgs843dMax;
    }

    @Override
    public IndexType type() {
        return IndexType.POINT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PointIndexOptions that = (PointIndexOptions) o;
        return Objects.equals(cartesianMin, that.cartesianMin)
                && Objects.equals(cartesianMax, that.cartesianMax)
                && Objects.equals(cartesian3dMin, that.cartesian3dMin)
                && Objects.equals(cartesian3dMax, that.cartesian3dMax)
                && Objects.equals(wgs84Min, that.wgs84Min)
                && Objects.equals(wgs84Max, that.wgs84Max)
                && Objects.equals(wgs843dMin, that.wgs843dMin)
                && Objects.equals(wgs843dMax, that.wgs843dMax);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                cartesianMin, cartesianMax, cartesian3dMin, cartesian3dMax, wgs84Min, wgs84Max, wgs843dMin, wgs843dMax);
    }

    @Override
    public String toString() {
        return "PointIndexOptions{" + "cartesianMin="
                + cartesianMin + ", cartesianMax="
                + cartesianMax + ", cartesian3dMin="
                + cartesian3dMin + ", cartesian3dMax="
                + cartesian3dMax + ", wgs84Min="
                + wgs84Min + ", wgs84Max="
                + wgs84Max + ", wgs843dMin="
                + wgs843dMin + ", wgs843dMax="
                + wgs843dMax + "}";
    }

    /** Builder for {@link PointIndexOptions}. */
    public static final class Builder {
        private final List<Double> cartesianMin = new ArrayList<>();
        private final List<Double> cartesianMax = new ArrayList<>();
        private final List<Double> cartesian3dMin = new ArrayList<>();
        private final List<Double> cartesian3dMax = new ArrayList<>();
        private final List<Double> wgs84Min = new ArrayList<>();
        private final List<Double> wgs84Max = new ArrayList<>();
        private final List<Double> wgs843dMin = new ArrayList<>();
        private final List<Double> wgs843dMax = new ArrayList<>();

        private Builder() {}

        private Builder(PointIndexOptions v) {
            this.cartesianMin.addAll(v.cartesianMin);
            this.cartesianMax.addAll(v.cartesianMax);
            this.cartesian3dMin.addAll(v.cartesian3dMin);
            this.cartesian3dMax.addAll(v.cartesian3dMax);
            this.wgs84Min.addAll(v.wgs84Min);
            this.wgs84Max.addAll(v.wgs84Max);
            this.wgs843dMin.addAll(v.wgs843dMin);
            this.wgs843dMax.addAll(v.wgs843dMax);
        }

        /**
         * Replaces lower bounds of the 2D cartesian space (wire key {@code spatial.cartesian.min}).
         *
         * @param cartesianMin the values, or null to clear
         * @return this builder
         */
        public Builder cartesianMin(List<Double> cartesianMin) {
            this.cartesianMin.clear();
            if (cartesianMin != null) {
                this.cartesianMin.addAll(cartesianMin);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder cartesianMinValue(Double value) {
            this.cartesianMin.add(Objects.requireNonNull(value, "cartesianMinValue"));
            return this;
        }

        /**
         * Replaces upper bounds of the 2D cartesian space (wire key {@code spatial.cartesian.max}).
         *
         * @param cartesianMax the values, or null to clear
         * @return this builder
         */
        public Builder cartesianMax(List<Double> cartesianMax) {
            this.cartesianMax.clear();
            if (cartesianMax != null) {
                this.cartesianMax.addAll(cartesianMax);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder cartesianMaxValue(Double value) {
            this.cartesianMax.add(Objects.requireNonNull(value, "cartesianMaxValue"));
            return this;
        }

        /**
         * Replaces lower bounds of the 3D cartesian space (wire key {@code spatial.cartesian-3d.min}).
         *
         * @param cartesian3dMin the values, or null to clear
         * @return this builder
         */
        public Builder cartesian3dMin(List<Double> cartesian3dMin) {
            this.cartesian3dMin.clear();
            if (cartesian3dMin != null) {
                this.cartesian3dMin.addAll(cartesian3dMin);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder cartesian3dMinValue(Double value) {
            this.cartesian3dMin.add(Objects.requireNonNull(value, "cartesian3dMinValue"));
            return this;
        }

        /**
         * Replaces upper bounds of the 3D cartesian space (wire key {@code spatial.cartesian-3d.max}).
         *
         * @param cartesian3dMax the values, or null to clear
         * @return this builder
         */
        public Builder cartesian3dMax(List<Double> cartesian3dMax) {
            this.cartesian3dMax.clear();
            if (cartesian3dMax != null) {
                this.cartesian3dMax.addAll(cartesian3dMax);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder cartesian3dMaxValue(Double value) {
            this.cartesian3dMax.add(Objects.requireNonNull(value, "cartesian3dMaxValue"));
            return this;
        }

        /**
         * Replaces lower bounds of the 2D WGS-84 space (wire key {@code spatial.wgs-84.min}).
         *
         * @param wgs84Min the values, or null to clear
         * @return this builder
         */
        public Builder wgs84Min(List<Double> wgs84Min) {
            this.wgs84Min.clear();
            if (wgs84Min != null) {
                this.wgs84Min.addAll(wgs84Min);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder wgs84MinValue(Double value) {
            this.wgs84Min.add(Objects.requireNonNull(value, "wgs84MinValue"));
            return this;
        }

        /**
         * Replaces upper bounds of the 2D WGS-84 space (wire key {@code spatial.wgs-84.max}).
         *
         * @param wgs84Max the values, or null to clear
         * @return this builder
         */
        public Builder wgs84Max(List<Double> wgs84Max) {
            this.wgs84Max.clear();
            if (wgs84Max != null) {
                this.wgs84Max.addAll(wgs84Max);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder wgs84MaxValue(Double value) {
            this.wgs84Max.add(Objects.requireNonNull(value, "wgs84MaxValue"));
            return this;
        }

        /**
         * Replaces lower bounds of the 3D WGS-84 space (wire key {@code spatial.wgs-84-3d.min}).
         *
         * @param wgs843dMin the values, or null to clear
         * @return this builder
         */
        public Builder wgs843dMin(List<Double> wgs843dMin) {
            this.wgs843dMin.clear();
            if (wgs843dMin != null) {
                this.wgs843dMin.addAll(wgs843dMin);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder wgs843dMinValue(Double value) {
            this.wgs843dMin.add(Objects.requireNonNull(value, "wgs843dMinValue"));
            return this;
        }

        /**
         * Replaces upper bounds of the 3D WGS-84 space (wire key {@code spatial.wgs-84-3d.max}).
         *
         * @param wgs843dMax the values, or null to clear
         * @return this builder
         */
        public Builder wgs843dMax(List<Double> wgs843dMax) {
            this.wgs843dMax.clear();
            if (wgs843dMax != null) {
                this.wgs843dMax.addAll(wgs843dMax);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder wgs843dMaxValue(Double value) {
            this.wgs843dMax.add(Objects.requireNonNull(value, "wgs843dMaxValue"));
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public PointIndexOptions build() {
            return new PointIndexOptions(this);
        }
    }
}
