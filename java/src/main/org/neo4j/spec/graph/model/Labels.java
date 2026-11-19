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
import java.util.Optional;

/** The labels of a node. */
public final class Labels {
    private final String identifier;
    private final List<String> implied;
    private final List<String> optional;
    private final LabelsExtensions extensions;

    private Labels(Builder b) {
        this.identifier = b.identifier;
        this.implied = Collections.unmodifiableList(new ArrayList<>(b.implied));
        this.optional = Collections.unmodifiableList(new ArrayList<>(b.optional));
        this.extensions = b.extensions;
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

    /** @return the label which identifies the node. */
    public Optional<String> identifier() {
        return Optional.ofNullable(identifier);
    }

    /** @return labels implied by the node in addition to the identifier. */
    public List<String> implied() {
        return implied;
    }

    /** @return labels which may be present on the node but are not required. */
    public List<String> optional() {
        return optional;
    }

    /** @return extensions for the labels. */
    public Optional<LabelsExtensions> extensions() {
        return Optional.ofNullable(extensions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Labels that = (Labels) o;
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
        return "Labels{" + "identifier="
                + identifier + ", implied="
                + implied + ", optional="
                + optional + ", extensions="
                + extensions + "}";
    }

    /** Builder for {@link Labels}. */
    public static final class Builder {
        private String identifier;
        private final List<String> implied = new ArrayList<>();
        private final List<String> optional = new ArrayList<>();
        private LabelsExtensions extensions;

        private Builder() {}

        private Builder(Labels v) {
            this.identifier = v.identifier;
            this.implied.addAll(v.implied);
            this.optional.addAll(v.optional);
            this.extensions = v.extensions;
        }

        /**
         * Sets the label which identifies the node.
         *
         * @param identifier the value, or null to clear it
         * @return this builder
         */
        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Replaces labels implied by the node in addition to the identifier.
         *
         * @param implied the values, or null to clear
         * @return this builder
         */
        public Builder implied(List<String> implied) {
            this.implied.clear();
            if (implied != null) {
                this.implied.addAll(implied);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder impliedLabel(String value) {
            this.implied.add(Objects.requireNonNull(value, "impliedLabel"));
            return this;
        }

        /**
         * Replaces labels which may be present on the node but are not required.
         *
         * @param optional the values, or null to clear
         * @return this builder
         */
        public Builder optional(List<String> optional) {
            this.optional.clear();
            if (optional != null) {
                this.optional.addAll(optional);
            }
            return this;
        }

        /**
         * Appends one item to the list.
         *
         * @param value the item
         * @return this builder
         */
        public Builder optionalLabel(String value) {
            this.optional.add(Objects.requireNonNull(value, "optionalLabel"));
            return this;
        }

        /**
         * Sets extensions for the labels.
         *
         * @param extensions the value, or null to clear it
         * @return this builder
         */
        public Builder extensions(LabelsExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the immutable instance
         * @throws IllegalStateException if a required field is missing
         */
        public Labels build() {
            return new Labels(this);
        }
    }
}
