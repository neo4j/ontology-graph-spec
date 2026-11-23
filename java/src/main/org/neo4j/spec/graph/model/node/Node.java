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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.node.extension.NodeExtensions;
import org.neo4j.spec.graph.model.property.Property;

@JsonClassDescription("A node of the graph.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class Node {
    private String label;

    private final Labels labels;

    private final Map<String, Property> properties;

    private final Map<String, NodeConstraint> constraints;

    private final NodeExtensions extensions;

    private final String description;

    private final List<String> aliases;

    private final String reference;

    private Node(
            String label,
            Labels labels,
            Map<String, Property> properties,
            Map<String, NodeConstraint> constraints,
            NodeExtensions extensions,
            String description,
            List<String> aliases,
            String reference) {
        this.label = label;
        this.labels = labels;
        this.properties = properties;
        this.constraints = constraints;
        this.extensions = extensions;
        this.description = description;
        this.aliases = aliases;
        this.reference = reference;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    public Labels getLabels() {
        return this.labels;
    }

    public Map<String, Property> getProperties() {
        return this.properties;
    }

    public Map<String, NodeConstraint> getConstraints() {
        return this.constraints;
    }

    public NodeExtensions getExtensions() {
        return this.extensions;
    }

    public String getDescription() {
        return this.description;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public String getReference() {
        return this.reference;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Node)) return false;
        Node that = (Node) other;
        return Objects.equals(label, that.label)
                && Objects.equals(labels, that.labels)
                && Objects.equals(properties, that.properties)
                && Objects.equals(constraints, that.constraints)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(description, that.description)
                && Objects.equals(aliases, that.aliases)
                && Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(labels);
        result = 31 * result + Objects.hashCode(properties);
        result = 31 * result + Objects.hashCode(constraints);
        result = 31 * result + Objects.hashCode(extensions);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(aliases);
        result = 31 * result + Objects.hashCode(reference);
        return result;
    }

    @Override
    public String toString() {
        return "Node(label=" + label + ", labels=" + labels + ", properties=" + properties + ", constraints="
                + constraints + ", extensions=" + extensions + ", description=" + description + ", aliases=" + aliases
                + ", reference=" + reference + ")";
    }

    @JsonProperty("label")
    @JsonPropertyDescription("Shorthand for the identifying label. Expanded into labels when the model is decoded.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonLabel() {
        return Objects.equals(this.label, null) ? null : this.label;
    }

    @JsonProperty("labels")
    @JsonPropertyDescription("Labels of the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Labels jsonLabels() {
        return Objects.equals(this.labels, Labels.builder().build()) ? null : this.labels;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Properties of the node keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Property> jsonProperties() {
        return Objects.equals(this.properties, new LinkedHashMap<String, Property>()) ? null : this.properties;
    }

    @JsonProperty("constraints")
    @JsonPropertyDescription("Constraints on the node keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, NodeConstraint> jsonConstraints() {
        return Objects.equals(this.constraints, new LinkedHashMap<String, NodeConstraint>()) ? null : this.constraints;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions for the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private NodeExtensions jsonExtensions() {
        return Objects.equals(this.extensions, NodeExtensions.builder().build()) ? null : this.extensions;
    }

    @JsonProperty("description")
    @JsonPropertyDescription("Description of the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonDescription() {
        return Objects.equals(this.description, "") ? null : this.description;
    }

    @JsonProperty("aliases")
    @JsonPropertyDescription("Alternative names for the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonAliases() {
        return Objects.equals(this.aliases, new ArrayList<String>()) ? null : this.aliases;
    }

    @JsonProperty("reference")
    @JsonPropertyDescription("Reference or URI of an external resource describing the node.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonReference() {
        return Objects.equals(this.reference, "") ? null : this.reference;
    }

    @JsonCreator
    private static Node fromJson(
            @JsonProperty("label") String label,
            @JsonProperty("labels") Labels labels,
            @JsonProperty("properties") Map<String, Property> properties,
            @JsonProperty("constraints") Map<String, NodeConstraint> constraints,
            @JsonProperty("extensions") NodeExtensions extensions,
            @JsonProperty("description") String description,
            @JsonProperty("aliases") List<String> aliases,
            @JsonProperty("reference") String reference) {
        return new Node(
                label,
                labels != null ? labels : Labels.builder().build(),
                properties != null ? properties : new LinkedHashMap<>(),
                constraints != null ? constraints : new LinkedHashMap<>(),
                extensions != null ? extensions : NodeExtensions.builder().build(),
                description != null ? description : "",
                aliases != null ? aliases : new ArrayList<>(),
                reference != null ? reference : "");
    }

    public static final class Builder {
        private String label = null;
        private Labels labels = Labels.builder().build();
        private Map<String, Property> properties = new LinkedHashMap<>();
        private Map<String, NodeConstraint> constraints = new LinkedHashMap<>();
        private NodeExtensions extensions = NodeExtensions.builder().build();
        private String description = "";
        private List<String> aliases = new ArrayList<>();
        private String reference = "";

        private Builder() {}

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder labels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public Builder properties(Map<String, Property> properties) {
            this.properties = properties;
            return this;
        }

        public Builder constraints(Map<String, NodeConstraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder extensions(NodeExtensions extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Node build() {
            return new Node(label, labels, properties, constraints, extensions, description, aliases, reference);
        }
    }
}
