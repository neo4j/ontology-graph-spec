// Generated from org/neo4j/spec/graph/model/relationship/Relationship.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.relationship;

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
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipExtensions;

@JsonClassDescription("A relationship of the graph.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public final class Relationship {
    private String type;

    private final RelationshipTarget from;

    private final RelationshipTarget to;

    private final Map<String, Property> properties;

    private final Map<String, RelationshipConstraint> constraints;

    private final RelationshipExtensions extensions;

    private final String description;

    private final List<String> aliases;

    private final String reference;

    public Relationship(String type, RelationshipTarget from, RelationshipTarget to,
            Map<String, Property> properties, Map<String, RelationshipConstraint> constraints,
            RelationshipExtensions extensions, String description, List<String> aliases,
            String reference) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.properties = properties;
        this.constraints = constraints;
        this.extensions = extensions;
        this.description = description;
        this.aliases = aliases;
        this.reference = reference;
    }

    public Relationship(String type, RelationshipTarget from, RelationshipTarget to,
            Map<String, Property> properties, Map<String, RelationshipConstraint> constraints,
            RelationshipExtensions extensions, String description, List<String> aliases) {
        this(type, from, to, properties, constraints, extensions, description, aliases, "");
    }

    public Relationship(String type, RelationshipTarget from, RelationshipTarget to,
            Map<String, Property> properties, Map<String, RelationshipConstraint> constraints,
            RelationshipExtensions extensions, String description) {
        this(type, from, to, properties, constraints, extensions, description, new ArrayList<String>(), "");
    }

    public Relationship(String type, RelationshipTarget from, RelationshipTarget to,
            Map<String, Property> properties, Map<String, RelationshipConstraint> constraints,
            RelationshipExtensions extensions) {
        this(type, from, to, properties, constraints, extensions, "", new ArrayList<String>(), "");
    }

    public Relationship(String type, RelationshipTarget from, RelationshipTarget to,
            Map<String, Property> properties, Map<String, RelationshipConstraint> constraints) {
        this(type, from, to, properties, constraints, new RelationshipExtensions(), "", new ArrayList<String>(), "");
    }

    public Relationship(String type, RelationshipTarget from, RelationshipTarget to,
            Map<String, Property> properties) {
        this(type, from, to, properties, new LinkedHashMap<String, RelationshipConstraint>(), new RelationshipExtensions(), "", new ArrayList<String>(), "");
    }

    public Relationship(String type, RelationshipTarget from, RelationshipTarget to) {
        this(type, from, to, new LinkedHashMap<String, Property>(), new LinkedHashMap<String, RelationshipConstraint>(), new RelationshipExtensions(), "", new ArrayList<String>(), "");
    }

    public String getType() {
        return this.type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public RelationshipTarget getFrom() {
        return this.from;
    }

    public RelationshipTarget getTo() {
        return this.to;
    }

    public Map<String, Property> getProperties() {
        return this.properties;
    }

    public Map<String, RelationshipConstraint> getConstraints() {
        return this.constraints;
    }

    public RelationshipExtensions getExtensions() {
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
        if (!(other instanceof Relationship)) return false;
        Relationship that = (Relationship) other;
        return Objects.equals(type, that.type)
                        && Objects.equals(from, that.from)
                        && Objects.equals(to, that.to)
                        && Objects.equals(properties, that.properties)
                        && Objects.equals(constraints, that.constraints)
                        && Objects.equals(extensions, that.extensions)
                        && Objects.equals(description, that.description)
                        && Objects.equals(aliases, that.aliases)
                        && Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(from);
        result = 31 * result + Objects.hashCode(to);
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
        return "Relationship(type=" + type + ", from=" + from + ", to=" + to + ", properties=" + properties + ", constraints=" + constraints + ", extensions=" + extensions + ", description=" + description + ", aliases=" + aliases + ", reference=" + reference + ")";
    }

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the relationship.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonType() {
        return this.type;
    }

    @JsonProperty("from")
    @JsonPropertyDescription("The node the relationship starts at.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private RelationshipTarget jsonFrom() {
        return this.from;
    }

    @JsonProperty("to")
    @JsonPropertyDescription("The node the relationship ends at.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private RelationshipTarget jsonTo() {
        return this.to;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Properties of the relationship keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Property> jsonProperties() {
        return Objects.equals(this.properties, new LinkedHashMap<String, Property>()) ? null : this.properties;
    }

    @JsonProperty("constraints")
    @JsonPropertyDescription("Constraints on the relationship keyed by id.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, RelationshipConstraint> jsonConstraints() {
        return Objects.equals(this.constraints, new LinkedHashMap<String, RelationshipConstraint>()) ? null : this.constraints;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions for the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RelationshipExtensions jsonExtensions() {
        return Objects.equals(this.extensions, new RelationshipExtensions()) ? null : this.extensions;
    }

    @JsonProperty("description")
    @JsonPropertyDescription("Description of the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonDescription() {
        return Objects.equals(this.description, "") ? null : this.description;
    }

    @JsonProperty("aliases")
    @JsonPropertyDescription("Alternative names for the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> jsonAliases() {
        return Objects.equals(this.aliases, new ArrayList<String>()) ? null : this.aliases;
    }

    @JsonProperty("reference")
    @JsonPropertyDescription("Reference or URI of an external resource describing the relationship.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonReference() {
        return Objects.equals(this.reference, "") ? null : this.reference;
    }

    @JsonCreator
    private static Relationship fromJson(@JsonProperty("type") String type,
            @JsonProperty("from") RelationshipTarget from,
            @JsonProperty("to") RelationshipTarget to,
            @JsonProperty("properties") Map<String, Property> properties,
            @JsonProperty("constraints") Map<String, RelationshipConstraint> constraints,
            @JsonProperty("extensions") RelationshipExtensions extensions,
            @JsonProperty("description") String description,
            @JsonProperty("aliases") List<String> aliases,
            @JsonProperty("reference") String reference) {
        return new Relationship(Objects.requireNonNull(type, "Field 'type' is required"),
                    Objects.requireNonNull(from, "Field 'from' is required"),
                    Objects.requireNonNull(to, "Field 'to' is required"),
                    properties != null ? properties : new LinkedHashMap<String, Property>(),
                    constraints != null ? constraints : new LinkedHashMap<String, RelationshipConstraint>(),
                    extensions != null ? extensions : new RelationshipExtensions(),
                    description != null ? description : "",
                    aliases != null ? aliases : new ArrayList<String>(),
                    reference != null ? reference : "");
    }
}
