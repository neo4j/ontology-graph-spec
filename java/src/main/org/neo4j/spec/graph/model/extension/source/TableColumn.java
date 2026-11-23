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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;
import org.neo4j.spec.graph.model.property.Neo4jType;

@JsonClassDescription("A column of a data source table.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class TableColumn implements CustomExtensions {
    private final String name;

    private String type;

    private int size;

    private final Neo4jType suggested;

    private final List<Neo4jType> supported;

    private final boolean nullable;

    private final Integer dimension;

    private final Map<String, Object> custom;

    private TableColumn(
            String name,
            String type,
            int size,
            Neo4jType suggested,
            List<Neo4jType> supported,
            boolean nullable,
            Integer dimension,
            Map<String, Object> custom) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.suggested = suggested;
        this.supported = supported;
        this.nullable = nullable;
        this.dimension = dimension;
        this.custom = custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int value) {
        this.size = value;
    }

    public Neo4jType getSuggested() {
        return this.suggested;
    }

    public List<Neo4jType> getSupported() {
        return this.supported;
    }

    public boolean getNullable() {
        return this.nullable;
    }

    public Integer getDimension() {
        return this.dimension;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TableColumn)) return false;
        TableColumn that = (TableColumn) other;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && size == that.size
                && Objects.equals(suggested, that.suggested)
                && Objects.equals(supported, that.supported)
                && nullable == that.nullable
                && Objects.equals(dimension, that.dimension)
                && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Integer.hashCode(size);
        result = 31 * result + Objects.hashCode(suggested);
        result = 31 * result + Objects.hashCode(supported);
        result = 31 * result + Boolean.hashCode(nullable);
        result = 31 * result + Objects.hashCode(dimension);
        result = 31 * result + Objects.hashCode(custom);
        return result;
    }

    @Override
    public String toString() {
        return "TableColumn(name=" + name + ", type=" + type + ", size=" + size + ", suggested=" + suggested
                + ", supported=" + supported + ", nullable=" + nullable + ", dimension=" + dimension + ", custom="
                + custom + ")";
    }

    @JsonProperty("name")
    @JsonPropertyDescription("Name of the column in the source. Unique within the table.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonName() {
        return Objects.equals(this.name, "") ? null : this.name;
    }

    @JsonProperty("type")
    @JsonPropertyDescription("Data type of the column in the source.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonType() {
        return Objects.equals(this.type, "") ? null : this.type;
    }

    @JsonProperty("size")
    @JsonPropertyDescription("Size of the column.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonSize() {
        return this.size == -1 ? null : this.size;
    }

    @JsonProperty("suggested")
    @JsonPropertyDescription("Neo4j type suggested for values of this column.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Neo4jType jsonSuggested() {
        return Objects.equals(this.suggested, Neo4jType.ANY) ? null : this.suggested;
    }

    @JsonProperty("supported")
    @JsonPropertyDescription("Neo4j types values of this column can be converted to when using the import tool.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Neo4jType> jsonSupported() {
        return Objects.equals(this.supported, Collections.<Neo4jType>emptyList()) ? null : this.supported;
    }

    @JsonProperty("nullable")
    @JsonPropertyDescription("Whether the column can hold null values.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean jsonNullable() {
        return this.nullable ? null : false;
    }

    @JsonProperty("dimension")
    @JsonPropertyDescription("Dimension of the column when it holds vectors.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonDimension() {
        return Objects.equals(this.dimension, null) ? null : this.dimension;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static TableColumn fromJson(
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("size") Integer size,
            @JsonProperty("suggested") Neo4jType suggested,
            @JsonProperty("supported") List<Neo4jType> supported,
            @JsonProperty("nullable") Boolean nullable,
            @JsonProperty("dimension") Integer dimension,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new TableColumn(
                name != null ? name : "",
                type != null ? type : "",
                size != null ? size : -1,
                suggested != null ? suggested : Neo4jType.ANY,
                supported != null ? supported : Collections.<Neo4jType>emptyList(),
                nullable != null ? nullable : true,
                dimension,
                custom != null ? custom : new LinkedHashMap<String, Object>());
    }

    public static final class Builder {
        private String name = "";
        private String type = "";
        private int size = -1;
        private Neo4jType suggested = Neo4jType.ANY;
        private List<Neo4jType> supported = Collections.<Neo4jType>emptyList();
        private boolean nullable = true;
        private Integer dimension = null;
        private Map<String, Object> custom = new LinkedHashMap<String, Object>();

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder suggested(Neo4jType suggested) {
            this.suggested = suggested;
            return this;
        }

        public Builder supported(List<Neo4jType> supported) {
            this.supported = supported;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder dimension(Integer dimension) {
            this.dimension = dimension;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public TableColumn build() {
            return new TableColumn(name, type, size, suggested, supported, nullable, dimension, custom);
        }
    }
}
