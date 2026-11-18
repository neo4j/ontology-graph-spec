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
package org.neo4j.spec.graph.model.property;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.property.extension.PropertyExtensions;
import org.neo4j.spec.graph.model.type.Named;

@JsonClassDescription("A property of a node or relationship.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class Property implements Named {
    private Neo4jType type;

    private Integer dimension;

    private Boolean mustExist;

    private Boolean unique;

    private Boolean key;

    private final PropertyExtensions extensions;

    private String name;

    private final String description;

    private final List<String> aliases;

    private final String reference;

    private final String pattern;

    private final List<String> oneOf;

    public Property(
            Neo4jType type,
            Integer dimension,
            Boolean mustExist,
            Boolean unique,
            Boolean key,
            PropertyExtensions extensions,
            String name,
            String description,
            List<String> aliases,
            String reference,
            String pattern,
            List<String> oneOf) {
        this.type = type;
        this.dimension = dimension;
        this.mustExist = mustExist;
        this.unique = unique;
        this.key = key;
        this.extensions = extensions;
        this.name = name;
        this.description = description;
        this.aliases = aliases;
        this.reference = reference;
        this.pattern = pattern;
        this.oneOf = oneOf;
    }

    public Property(
            Neo4jType type,
            Integer dimension,
            Boolean mustExist,
            Boolean unique,
            Boolean key,
            PropertyExtensions extensions,
            String name,
            String description,
            List<String> aliases,
            String reference,
            String pattern) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                key,
                extensions,
                name,
                description,
                aliases,
                reference,
                pattern,
                new ArrayList<String>());
    }

    public Property(
            Neo4jType type,
            Integer dimension,
            Boolean mustExist,
            Boolean unique,
            Boolean key,
            PropertyExtensions extensions,
            String name,
            String description,
            List<String> aliases,
            String reference) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                key,
                extensions,
                name,
                description,
                aliases,
                reference,
                "",
                new ArrayList<String>());
    }

    public Property(
            Neo4jType type,
            Integer dimension,
            Boolean mustExist,
            Boolean unique,
            Boolean key,
            PropertyExtensions extensions,
            String name,
            String description,
            List<String> aliases) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                key,
                extensions,
                name,
                description,
                aliases,
                "",
                "",
                new ArrayList<String>());
    }

    public Property(
            Neo4jType type,
            Integer dimension,
            Boolean mustExist,
            Boolean unique,
            Boolean key,
            PropertyExtensions extensions,
            String name,
            String description) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                key,
                extensions,
                name,
                description,
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property(
            Neo4jType type,
            Integer dimension,
            Boolean mustExist,
            Boolean unique,
            Boolean key,
            PropertyExtensions extensions,
            String name) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                key,
                extensions,
                name,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property(
            Neo4jType type,
            Integer dimension,
            Boolean mustExist,
            Boolean unique,
            Boolean key,
            PropertyExtensions extensions) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                key,
                extensions,
                null,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property(Neo4jType type, Integer dimension, Boolean mustExist, Boolean unique, Boolean key) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                key,
                new PropertyExtensions(),
                null,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property(Neo4jType type, Integer dimension, Boolean mustExist, Boolean unique) {
        this(
                type,
                dimension,
                mustExist,
                unique,
                null,
                new PropertyExtensions(),
                null,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property(Neo4jType type, Integer dimension, Boolean mustExist) {
        this(
                type,
                dimension,
                mustExist,
                null,
                null,
                new PropertyExtensions(),
                null,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property(Neo4jType type, Integer dimension) {
        this(
                type,
                dimension,
                null,
                null,
                null,
                new PropertyExtensions(),
                null,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property(Neo4jType type) {
        this(
                type,
                null,
                null,
                null,
                null,
                new PropertyExtensions(),
                null,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Property() {
        this(
                Neo4jType.ANY,
                null,
                null,
                null,
                null,
                new PropertyExtensions(),
                null,
                "",
                new ArrayList<String>(),
                "",
                "",
                new ArrayList<String>());
    }

    public Neo4jType getType() {
        return this.type;
    }

    public void setType(Neo4jType value) {
        this.type = value;
    }

    public Integer getDimension() {
        return this.dimension;
    }

    public void setDimension(Integer value) {
        this.dimension = value;
    }

    public Boolean getMustExist() {
        return this.mustExist;
    }

    public void setMustExist(Boolean value) {
        this.mustExist = value;
    }

    public Boolean getUnique() {
        return this.unique;
    }

    public void setUnique(Boolean value) {
        this.unique = value;
    }

    public Boolean getKey() {
        return this.key;
    }

    public void setKey(Boolean value) {
        this.key = value;
    }

    public PropertyExtensions getExtensions() {
        return this.extensions;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
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

    public String getPattern() {
        return this.pattern;
    }

    public List<String> getOneOf() {
        return this.oneOf;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Property)) return false;
        Property that = (Property) other;
        return Objects.equals(type, that.type)
                && Objects.equals(dimension, that.dimension)
                && Objects.equals(mustExist, that.mustExist)
                && Objects.equals(unique, that.unique)
                && Objects.equals(key, that.key)
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(aliases, that.aliases)
                && Objects.equals(reference, that.reference)
                && Objects.equals(pattern, that.pattern)
                && Objects.equals(oneOf, that.oneOf);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(dimension);
        result = 31 * result + Objects.hashCode(mustExist);
        result = 31 * result + Objects.hashCode(unique);
        result = 31 * result + Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(extensions);
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(aliases);
        result = 31 * result + Objects.hashCode(reference);
        result = 31 * result + Objects.hashCode(pattern);
        result = 31 * result + Objects.hashCode(oneOf);
        return result;
    }

    @Override
    public String toString() {
        return "Property(type=" + type + ", dimension=" + dimension + ", mustExist=" + mustExist + ", unique=" + unique
                + ", key=" + key + ", extensions=" + extensions + ", name=" + name + ", description=" + description
                + ", aliases=" + aliases + ", reference=" + reference + ", pattern=" + pattern + ", oneOf=" + oneOf
                + ")";
    }

    @JsonProperty("type")
    @JsonPropertyDescription("Neo4j type of the property.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Neo4jType jsonType() {
        return Objects.equals(this.type, Neo4jType.ANY) ? null : this.type;
    }

    @JsonProperty("dimension")
    @JsonPropertyDescription("Dimension of the property when it holds a vector.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonDimension() {
        return Objects.equals(this.dimension, null) ? null : this.dimension;
    }

    @JsonProperty("must_exist")
    @JsonPropertyDescription(
            "Shorthand for an existence constraint. Expanded into constraints when the model is decoded.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean jsonMustExist() {
        return Objects.equals(this.mustExist, null) ? null : this.mustExist;
    }

    @JsonProperty("unique")
    @JsonPropertyDescription(
            "Shorthand for a uniqueness constraint. Expanded into constraints when the model is decoded.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean jsonUnique() {
        return Objects.equals(this.unique, null) ? null : this.unique;
    }

    @JsonProperty("key")
    @JsonPropertyDescription("Shorthand for a key constraint. Expanded into constraints when the model is decoded.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean jsonKey() {
        return Objects.equals(this.key, null) ? null : this.key;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions for the property.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PropertyExtensions jsonExtensions() {
        return Objects.equals(this.extensions, new PropertyExtensions()) ? null : this.extensions;
    }

    @JsonProperty("name")
    @JsonPropertyDescription("Name of the property in the database, when different from its id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonName() {
        return Objects.equals(this.name, null) ? null : this.name;
    }

    @JsonProperty("description")
    @JsonPropertyDescription("Description of the property.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonDescription() {
        return Objects.equals(this.description, "") ? null : this.description;
    }

    @JsonProperty("aliases")
    @JsonPropertyDescription("Alternative names for the property.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonAliases() {
        return Objects.equals(this.aliases, new ArrayList<String>()) ? null : this.aliases;
    }

    @JsonProperty("reference")
    @JsonPropertyDescription("Reference or URI of an external resource describing the property.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonReference() {
        return Objects.equals(this.reference, "") ? null : this.reference;
    }

    @JsonProperty("pattern")
    @JsonPropertyDescription("Pattern values of the property are expected to match.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonPattern() {
        return Objects.equals(this.pattern, "") ? null : this.pattern;
    }

    @JsonProperty("one_of")
    @JsonPropertyDescription("Values the property is restricted to.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonOneOf() {
        return Objects.equals(this.oneOf, new ArrayList<String>()) ? null : this.oneOf;
    }

    @JsonCreator
    private static Property fromJson(
            @JsonProperty("type") Neo4jType type,
            @JsonProperty("dimension") Integer dimension,
            @JsonProperty("must_exist") Boolean mustExist,
            @JsonProperty("unique") Boolean unique,
            @JsonProperty("key") Boolean key,
            @JsonProperty("extensions") PropertyExtensions extensions,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("aliases") List<String> aliases,
            @JsonProperty("reference") String reference,
            @JsonProperty("pattern") String pattern,
            @JsonProperty("one_of") List<String> oneOf) {
        return new Property(
                type != null ? type : Neo4jType.ANY,
                dimension != null ? dimension : null,
                mustExist != null ? mustExist : null,
                unique != null ? unique : null,
                key != null ? key : null,
                extensions != null ? extensions : new PropertyExtensions(),
                name != null ? name : null,
                description != null ? description : "",
                aliases != null ? aliases : new ArrayList<String>(),
                reference != null ? reference : "",
                pattern != null ? pattern : "",
                oneOf != null ? oneOf : new ArrayList<String>());
    }
}
