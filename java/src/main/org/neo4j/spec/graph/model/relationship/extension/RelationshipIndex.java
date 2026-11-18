// Generated from org/neo4j/spec/graph/model/relationship/extension/RelationshipIndex.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.relationship.extension;

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
import org.neo4j.spec.graph.model.index.IndexOption;
import org.neo4j.spec.graph.model.type.IndexType;
import org.neo4j.spec.graph.model.type.Named;

@JsonClassDescription("An index on a relationship.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public final class RelationshipIndex implements CustomExtensions, Named {
    private IndexType type;

    private final List<String> properties;

    private final IndexOption options;

    private final Map<String, Object> custom;

    private String name;

    public RelationshipIndex(IndexType type, List<String> properties, IndexOption options,
            Map<String, Object> custom, String name) {
        this.type = type;
        this.properties = properties;
        this.options = options;
        this.custom = custom;
        this.name = name;
    }

    public RelationshipIndex(IndexType type, List<String> properties, IndexOption options,
            Map<String, Object> custom) {
        this(type, properties, options, custom, null);
    }

    public RelationshipIndex(IndexType type, List<String> properties, IndexOption options) {
        this(type, properties, options, new LinkedHashMap<String, Object>(), null);
    }

    public RelationshipIndex(IndexType type, List<String> properties) {
        this(type, properties, null, new LinkedHashMap<String, Object>(), null);
    }

    public IndexType getType() {
        return this.type;
    }

    public void setType(IndexType value) {
        this.type = value;
    }

    public List<String> getProperties() {
        return this.properties;
    }

    public IndexOption getOptions() {
        return this.options;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RelationshipIndex)) return false;
        RelationshipIndex that = (RelationshipIndex) other;
        return Objects.equals(type, that.type)
                        && Objects.equals(properties, that.properties)
                        && Objects.equals(options, that.options)
                        && Objects.equals(custom, that.custom)
                        && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(properties);
        result = 31 * result + Objects.hashCode(options);
        result = 31 * result + Objects.hashCode(custom);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }

    @Override
    public String toString() {
        return "RelationshipIndex(type=" + type + ", properties=" + properties + ", options=" + options + ", custom=" + custom + ", name=" + name + ")";
    }

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the index.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private IndexType jsonType() {
        return this.type;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Ids of the properties the index covers.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<String> jsonProperties() {
        return this.properties;
    }

    @JsonProperty("options")
    @JsonPropertyDescription("Configuration specific to the type of index.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private IndexOption jsonOptions() {
        return Objects.equals(this.options, null) ? null : this.options;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonProperty("name")
    @JsonPropertyDescription("Name of the index in the database.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonName() {
        return Objects.equals(this.name, null) ? null : this.name;
    }

    @JsonCreator
    private static RelationshipIndex fromJson(@JsonProperty("type") IndexType type,
            @JsonProperty("properties") List<String> properties,
            @JsonProperty("options") IndexOption options,
            @JsonProperty("custom") Map<String, Object> custom, @JsonProperty("name") String name) {
        return new RelationshipIndex(Objects.requireNonNull(type, "Field 'type' is required"),
                    Objects.requireNonNull(properties, "Field 'properties' is required"),
                    options != null ? options : null,
                    custom != null ? custom : new LinkedHashMap<String, Object>(),
                    name != null ? name : null);
    }
}
