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
package org.neo4j.spec.graph.model.extension.mapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Identifies a node at one end of a mapped relationship. */
public final class TargetMapping {
    private String label;

    private final Map<String, PropertyMapping> properties;

    private TargetMapping(Builder builder) {
        this.label = builder.label;
        this.properties = builder.properties;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Label of the node. */
    public String getLabel() {
        return this.label;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    /** Mappings from property ids to source columns used to match the node. */
    public Map<String, PropertyMapping> getProperties() {
        return this.properties;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TargetMapping)) return false;
        TargetMapping that = (TargetMapping) other;
        return Objects.equals(label, that.label) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, properties);
    }

    @Override
    public String toString() {
        return "TargetMapping(label=" + label + ", properties=" + properties + ")";
    }

    public static final class Builder {
        private String label = "";
        private Map<String, PropertyMapping> properties = new LinkedHashMap<>();

        private Builder() {}

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder properties(Map<String, PropertyMapping> properties) {
            this.properties = properties;
            return this;
        }

        public TargetMapping build() {
            return new TargetMapping(this);
        }
    }
}
