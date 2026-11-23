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
package org.neo4j.spec.graph.model.extension.source;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

@JsonClassDescription("A foreign key of a table.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class ForeignKey implements CustomExtensions {
    private final List<String> columns;

    private final ForeignKeyReference references;

    private final Map<String, Object> custom;

    private ForeignKey(List<String> columns, ForeignKeyReference references, Map<String, Object> custom) {
        this.columns = columns;
        this.references = references;
        this.custom = custom;
    }

    public static Builder builder(List<String> columns, ForeignKeyReference references) {
        return new Builder(columns, references);
    }

    public List<String> getColumns() {
        return this.columns;
    }

    public ForeignKeyReference getReferences() {
        return this.references;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ForeignKey)) return false;
        ForeignKey that = (ForeignKey) other;
        return Objects.equals(columns, that.columns)
                && Objects.equals(references, that.references)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, references, custom);
    }

    @Override
    public String toString() {
        return "ForeignKey(columns=" + columns + ", references=" + references + ", custom=" + custom + ")";
    }

    @JsonProperty("columns")
    @JsonPropertyDescription("Names of the columns in this table which make up the key.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<String> jsonColumns() {
        return this.columns;
    }

    @JsonProperty("references")
    @JsonPropertyDescription("The table and columns the key refers to.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private ForeignKeyReference jsonReferences() {
        return this.references;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static ForeignKey fromJson(
            @JsonProperty("columns") List<String> columns,
            @JsonProperty("references") ForeignKeyReference references,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new ForeignKey(
                Objects.requireNonNull(columns, "Field 'columns' is required"),
                Objects.requireNonNull(references, "Field 'references' is required"),
                custom != null ? custom : new LinkedHashMap<>());
    }

    public static final class Builder {
        private List<String> columns;
        private ForeignKeyReference references;
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder(List<String> columns, ForeignKeyReference references) {
            this.columns = columns;
            this.references = references;
        }

        public Builder columns(List<String> columns) {
            this.columns = columns;
            return this;
        }

        public Builder references(ForeignKeyReference references) {
            this.references = references;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public ForeignKey build() {
            return new ForeignKey(columns, references, custom);
        }
    }
}
