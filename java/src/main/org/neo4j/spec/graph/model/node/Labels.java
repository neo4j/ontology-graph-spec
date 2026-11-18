// Generated from org/neo4j/spec/graph/model/node/Labels.kt by :java:generator. Do not edit.
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
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public final class Labels {
    private String identifier;

    private final List<String> implied;

    private final List<String> optional;

    private final LabelsExtensions extensions;

    public Labels(String identifier, List<String> implied, List<String> optional,
            LabelsExtensions extensions) {
        this.identifier = identifier;
        this.implied = implied;
        this.optional = optional;
        this.extensions = extensions;
    }

    public Labels(String identifier, List<String> implied, List<String> optional) {
        this(identifier, implied, optional, new LabelsExtensions());
    }

    public Labels(String identifier, List<String> implied) {
        this(identifier, implied, new ArrayList<String>(), new LabelsExtensions());
    }

    public Labels(String identifier) {
        this(identifier, new ArrayList<String>(), new ArrayList<String>(), new LabelsExtensions());
    }

    public Labels() {
        this(null, new ArrayList<String>(), new ArrayList<String>(), new LabelsExtensions());
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
        int result = Objects.hashCode(identifier);
        result = 31 * result + Objects.hashCode(implied);
        result = 31 * result + Objects.hashCode(optional);
        result = 31 * result + Objects.hashCode(extensions);
        return result;
    }

    @Override
    public String toString() {
        return "Labels(identifier=" + identifier + ", implied=" + implied + ", optional=" + optional + ", extensions=" + extensions + ")";
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
        return Objects.equals(this.extensions, new LabelsExtensions()) ? null : this.extensions;
    }

    @JsonCreator
    private static Labels fromJson(@JsonProperty("identifier") String identifier,
            @JsonProperty("implied") List<String> implied,
            @JsonProperty("optional") List<String> optional,
            @JsonProperty("extensions") LabelsExtensions extensions) {
        return new Labels(identifier != null ? identifier : null,
                    implied != null ? implied : new ArrayList<String>(),
                    optional != null ? optional : new ArrayList<String>(),
                    extensions != null ? extensions : new LabelsExtensions());
    }
}
