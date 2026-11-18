// Generated from org/neo4j/spec/graph/model/relationship/extension/RelationshipExtensions.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.relationship.extension;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

@JsonClassDescription("Extensions for a relationship.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public final class RelationshipExtensions implements CustomExtensions {
    private final Map<String, RelationshipIndex> indexes;

    private final Map<String, Object> custom;

    public RelationshipExtensions(Map<String, RelationshipIndex> indexes,
            Map<String, Object> custom) {
        this.indexes = indexes;
        this.custom = custom;
    }

    public RelationshipExtensions(Map<String, RelationshipIndex> indexes) {
        this(indexes, new LinkedHashMap<String, Object>());
    }

    public RelationshipExtensions() {
        this(new LinkedHashMap<String, RelationshipIndex>(), new LinkedHashMap<String, Object>());
    }

    public Map<String, RelationshipIndex> getIndexes() {
        return this.indexes;
    }

    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RelationshipExtensions)) return false;
        RelationshipExtensions that = (RelationshipExtensions) other;
        return Objects.equals(indexes, that.indexes)
                        && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(indexes);
        result = 31 * result + Objects.hashCode(custom);
        return result;
    }

    @Override
    public String toString() {
        return "RelationshipExtensions(indexes=" + indexes + ", custom=" + custom + ")";
    }

    @JsonProperty("indexes")
    @JsonPropertyDescription("Indexes on this relationship keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, RelationshipIndex> jsonIndexes() {
        return Objects.equals(this.indexes, new LinkedHashMap<String, RelationshipIndex>()) ? null : this.indexes;
    }

    @JsonProperty("custom")
    @JsonPropertyDescription("User-defined arbitrary data.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> jsonCustom() {
        return Objects.equals(this.custom, new LinkedHashMap<String, Object>()) ? null : this.custom;
    }

    @JsonCreator
    private static RelationshipExtensions fromJson(
            @JsonProperty("indexes") Map<String, RelationshipIndex> indexes,
            @JsonProperty("custom") Map<String, Object> custom) {
        return new RelationshipExtensions(indexes != null ? indexes : new LinkedHashMap<String, RelationshipIndex>(),
                    custom != null ? custom : new LinkedHashMap<String, Object>());
    }
}
