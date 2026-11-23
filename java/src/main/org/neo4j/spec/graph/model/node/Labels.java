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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.node.extension.LabelsExtensions;

@JsonClassDescription("The labels of a node.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class Labels {
    private String identifier;

    private final List<String> implied;

    private final List<String> optional;

    private final LabelsExtensions extensions;

    private Labels(String identifier, List<String> implied, List<String> optional, LabelsExtensions extensions) {
        this.identifier = identifier;
        this.implied = implied;
        this.optional = optional;
        this.extensions = extensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String value) {
        this.identifier = value;
    }

    public List<String> getImplied() {
        return this.implied;
    }

    public List<String> getOptional() {
        return this.optional;
    }

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

    @JsonProperty("identifier")
    @JsonPropertyDescription("The label which identifies the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonIdentifier() {
        return Objects.equals(this.identifier, null) ? null : this.identifier;
    }

    @JsonProperty("implied")
    @JsonPropertyDescription("Labels implied by the node in addition to the identifier.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonImplied() {
        return Objects.equals(this.implied, new ArrayList<String>()) ? null : this.implied;
    }

    @JsonProperty("optional")
    @JsonPropertyDescription("Labels which may be present on the node but aren't required.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonOptional() {
        return Objects.equals(this.optional, new ArrayList<String>()) ? null : this.optional;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions for the labels.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LabelsExtensions jsonExtensions() {
        return Objects.equals(this.extensions, LabelsExtensions.builder().build()) ? null : this.extensions;
    }

    @JsonCreator
    private static Labels fromJson(
            @JsonProperty("identifier") String identifier,
            @JsonProperty("implied") List<String> implied,
            @JsonProperty("optional") List<String> optional,
            @JsonProperty("extensions") LabelsExtensions extensions) {
        return new Labels(
                identifier,
                implied != null ? implied : new ArrayList<>(),
                optional != null ? optional : new ArrayList<>(),
                extensions != null ? extensions : LabelsExtensions.builder().build());
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
            return new Labels(identifier, implied, optional, extensions);
        }
    }
}
