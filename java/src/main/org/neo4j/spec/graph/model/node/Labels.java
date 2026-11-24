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
package org.neo4j.spec.graph.model.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.node.extension.LabelsExtensions;

/** The labels of a node. */
public final class Labels {
    private String identifier;

    private final List<String> implied;

    private final List<String> optional;

    private final LabelsExtensions extensions;

    private Labels(Builder builder) {
        this.identifier = builder.identifier;
        this.implied = builder.implied;
        this.optional = builder.optional;
        this.extensions = builder.extensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The label which identifies the node. */
    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String value) {
        this.identifier = value;
    }

    /** Labels implied by the node in addition to the identifier. */
    public List<String> getImplied() {
        return this.implied;
    }

    /** Labels which may be present on the node but aren't required. */
    public List<String> getOptional() {
        return this.optional;
    }

    /** Extensions for the labels. */
    public LabelsExtensions getExtensions() {
        return this.extensions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Labels)) return false;
        Labels that = (Labels) other;
        return Objects.equals(identifier, that.identifier)
                && Objects.equals(implied, that.implied)
                && Objects.equals(optional, that.optional)
                && Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, implied, optional, extensions);
    }

    @Override
    public String toString() {
        return "Labels(identifier=" + identifier + ", implied=" + implied + ", optional=" + optional + ", extensions="
                + extensions + ")";
    }

    public static final class Builder {
        private String identifier = null;
        private List<String> implied = new ArrayList<>();
        private List<String> optional = new ArrayList<>();
        private LabelsExtensions extensions = LabelsExtensions.builder().build();

        private Builder() {}

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder implied(List<String> implied) {
            this.implied = implied;
            return this;
        }

        public Builder optional(List<String> optional) {
            this.optional = optional;
            return this;
        }

        public Builder extensions(LabelsExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        public Labels build() {
            return new Labels(this);
        }
    }
}
