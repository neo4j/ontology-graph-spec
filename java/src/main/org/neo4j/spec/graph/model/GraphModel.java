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
// Generated from org/neo4j/spec/graph/model/GraphModel.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.GraphModelExtensions;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.ValidationTree;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class GraphModel {
    private final String schema;

    private final String version;

    private final String id;

    private final String name;

    private final String description;

    private final Map<String, Node> nodes;

    private final Map<String, Relationship> relationships;

    private final GraphModelExtensions extensions;

    public GraphModel(
            String schema,
            String version,
            String id,
            String name,
            String description,
            Map<String, Node> nodes,
            Map<String, Relationship> relationships,
            GraphModelExtensions extensions) {
        this.schema = schema;
        this.version = version;
        this.id = id;
        this.name = name;
        this.description = description;
        this.nodes = nodes;
        this.relationships = relationships;
        this.extensions = extensions;
    }

    public GraphModel(
            String schema,
            String version,
            String id,
            String name,
            String description,
            Map<String, Node> nodes,
            Map<String, Relationship> relationships) {
        this(schema, version, id, name, description, nodes, relationships, new GraphModelExtensions());
    }

    public GraphModel(
            String schema, String version, String id, String name, String description, Map<String, Node> nodes) {
        this(
                schema,
                version,
                id,
                name,
                description,
                nodes,
                new LinkedHashMap<String, Relationship>(),
                new GraphModelExtensions());
    }

    public GraphModel(String schema, String version, String id, String name, String description) {
        this(
                schema,
                version,
                id,
                name,
                description,
                new LinkedHashMap<String, Node>(),
                new LinkedHashMap<String, Relationship>(),
                new GraphModelExtensions());
    }

    public GraphModel(String schema, String version, String id, String name) {
        this(
                schema,
                version,
                id,
                name,
                "",
                new LinkedHashMap<String, Node>(),
                new LinkedHashMap<String, Relationship>(),
                new GraphModelExtensions());
    }

    public GraphModel(String schema, String version, String id) {
        this(
                schema,
                version,
                id,
                "",
                "",
                new LinkedHashMap<String, Node>(),
                new LinkedHashMap<String, Relationship>(),
                new GraphModelExtensions());
    }

    public GraphModel(String version, String id) {
        this(
                Version.schemaUrl(Version.LATEST),
                version,
                id,
                "",
                "",
                new LinkedHashMap<String, Node>(),
                new LinkedHashMap<String, Relationship>(),
                new GraphModelExtensions());
    }

    public String getSchema() {
        return this.schema;
    }

    public String getVersion() {
        return this.version;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Map<String, Node> getNodes() {
        return this.nodes;
    }

    public Map<String, Relationship> getRelationships() {
        return this.relationships;
    }

    public GraphModelExtensions getExtensions() {
        return this.extensions;
    }

    /**
     * Expands any shorthand in the model into its longhand form, see {@code Notation}.
     */
    public void longhand() {
        Notation.longhand(this);
    }

    /**
     * Collapses longhand in the model into shorthand wherever that loses no information, see {@code Notation}.
     */
    public void shorthand() {
        Notation.shorthand(this);
    }

    public List<Issue> validate(List<? extends Validation> validators) {
        ValidationTree tree = new ValidationTree();
        tree.build(((List<Validation>) validators));
        return tree.validate(this);
    }

    public static List<Issue> validate(GraphModel model, List<? extends Validation> validators) {
        return model.validate(((List<Validation>) validators));
    }

    public static void longhand(GraphModel model) {
        model.longhand();
    }

    public static void shorthand(GraphModel model) {
        model.shorthand();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GraphModel)) return false;
        GraphModel that = (GraphModel) other;
        return Objects.equals(schema, that.schema)
                && Objects.equals(version, that.version)
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(nodes, that.nodes)
                && Objects.equals(relationships, that.relationships)
                && Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(schema);
        result = 31 * result + Objects.hashCode(version);
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(nodes);
        result = 31 * result + Objects.hashCode(relationships);
        result = 31 * result + Objects.hashCode(extensions);
        return result;
    }

    @Override
    public String toString() {
        return "GraphModel(schema=" + schema + ", version=" + version + ", id=" + id + ", name=" + name
                + ", description=" + description + ", nodes=" + nodes + ", relationships=" + relationships
                + ", extensions=" + extensions + ")";
    }

    @JsonProperty("schema")
    @JsonPropertyDescription("Schema specification uri used.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonSchema() {
        return this.schema;
    }

    @JsonProperty("version")
    @JsonPropertyDescription("User-defined version of this model document, e.g. a revision number or label.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonVersion() {
        return this.version;
    }

    @JsonProperty("id")
    @JsonPropertyDescription(
            "Unique identifier of the model, typically a kebab-case version of the name, used for referencing.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonId() {
        return this.id;
    }

    @JsonProperty("name")
    @JsonPropertyDescription("Human readable name for the model.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonName() {
        return Objects.equals(this.name, "") ? null : this.name;
    }

    @JsonProperty("description")
    @JsonPropertyDescription("Description of the model.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonDescription() {
        return Objects.equals(this.description, "") ? null : this.description;
    }

    @JsonProperty("nodes")
    @JsonPropertyDescription("Node definitions keyed by node id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Node> jsonNodes() {
        return Objects.equals(this.nodes, new LinkedHashMap<String, Node>()) ? null : this.nodes;
    }

    @JsonProperty("relationships")
    @JsonPropertyDescription("Relationship definitions keyed by relationship id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Relationship> jsonRelationships() {
        return Objects.equals(this.relationships, new LinkedHashMap<String, Relationship>())
                ? null
                : this.relationships;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions to the model: data source tables, mappings and external custom data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private GraphModelExtensions jsonExtensions() {
        return Objects.equals(this.extensions, new GraphModelExtensions()) ? null : this.extensions;
    }

    @JsonCreator
    private static GraphModel fromJson(
            @JsonProperty("schema") String schema,
            @JsonProperty("version") String version,
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("nodes") Map<String, Node> nodes,
            @JsonProperty("relationships") Map<String, Relationship> relationships,
            @JsonProperty("extensions") GraphModelExtensions extensions) {
        return new GraphModel(
                schema != null ? schema : Version.schemaUrl(Version.LATEST),
                Objects.requireNonNull(version, "Field 'version' is required"),
                Objects.requireNonNull(id, "Field 'id' is required"),
                name != null ? name : "",
                description != null ? description : "",
                nodes != null ? nodes : new LinkedHashMap<String, Node>(),
                relationships != null ? relationships : new LinkedHashMap<String, Relationship>(),
                extensions != null ? extensions : new GraphModelExtensions());
    }
}
