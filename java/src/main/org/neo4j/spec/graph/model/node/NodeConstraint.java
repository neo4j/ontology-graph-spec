// Generated from org/neo4j/spec/graph/model/node/NodeConstraint.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.node;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.node.extension.NodeConstraintExtensions;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.Named;

@JsonClassDescription("A constraint on a node.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public final class NodeConstraint implements Constraint, Named {
    private ConstraintType type;

    private final List<String> properties;

    private final NodeConstraintExtensions extensions;

    private String name;

    public NodeConstraint(ConstraintType type, List<String> properties,
            NodeConstraintExtensions extensions, String name) {
        this.type = type;
        this.properties = properties;
        this.extensions = extensions;
        this.name = name;
    }

    public NodeConstraint(ConstraintType type, List<String> properties,
            NodeConstraintExtensions extensions) {
        this(type, properties, extensions, null);
    }

    public NodeConstraint(ConstraintType type, List<String> properties) {
        this(type, properties, new NodeConstraintExtensions(), null);
    }

    @Override
    public ConstraintType getType() {
        return this.type;
    }

    public void setType(ConstraintType value) {
        this.type = value;
    }

    @Override
    public List<String> getProperties() {
        return this.properties;
    }

    public NodeConstraintExtensions getExtensions() {
        return this.extensions;
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
        if (!(other instanceof NodeConstraint)) return false;
        NodeConstraint that = (NodeConstraint) other;
        return Objects.equals(type, that.type)
                        && Objects.equals(properties, that.properties)
                        && Objects.equals(extensions, that.extensions)
                        && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(properties);
        result = 31 * result + Objects.hashCode(extensions);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }

    @Override
    public String toString() {
        return "NodeConstraint(type=" + type + ", properties=" + properties + ", extensions=" + extensions + ", name=" + name + ")";
    }

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the constraint.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private ConstraintType jsonType() {
        return this.type;
    }

    @JsonProperty("properties")
    @JsonPropertyDescription("Ids of the properties the constraint covers.")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<String> jsonProperties() {
        return this.properties;
    }

    @JsonProperty("extensions")
    @JsonPropertyDescription("Extensions for the constraint.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private NodeConstraintExtensions jsonExtensions() {
        return Objects.equals(this.extensions, new NodeConstraintExtensions()) ? null : this.extensions;
    }

    @JsonProperty("name")
    @JsonPropertyDescription("Name of the constraint in the database.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonName() {
        return Objects.equals(this.name, null) ? null : this.name;
    }

    @JsonCreator
    private static NodeConstraint fromJson(@JsonProperty("type") ConstraintType type,
            @JsonProperty("properties") List<String> properties,
            @JsonProperty("extensions") NodeConstraintExtensions extensions,
            @JsonProperty("name") String name) {
        return new NodeConstraint(Objects.requireNonNull(type, "Field 'type' is required"),
                    Objects.requireNonNull(properties, "Field 'properties' is required"),
                    extensions != null ? extensions : new NodeConstraintExtensions(),
                    name != null ? name : null);
    }
}
