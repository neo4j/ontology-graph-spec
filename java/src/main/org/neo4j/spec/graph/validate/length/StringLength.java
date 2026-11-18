// Generated from org/neo4j/spec/graph/validate/length/StringLength.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.length;

import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.type.Named;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;

/**
 * Restricts the length of names and descriptions throughout the model.
 */
public final class StringLength implements Validation {
    public static final StringLength INSTANCE = new StringLength();

    public final int MAX_NAME_LENGTH;

    public final int MAX_CONSTRAINT_INDEX_NAME_LENGTH;

    public final int MAX_MODEL_DESCRIPTION_LENGTH;

    public final int MAX_DESCRIPTION_LENGTH;

    private StringLength() {
        this.MAX_NAME_LENGTH = 256;
        this.MAX_CONSTRAINT_INDEX_NAME_LENGTH = 800;
        this.MAX_MODEL_DESCRIPTION_LENGTH = 64 * 1024;
        this.MAX_DESCRIPTION_LENGTH = 1024;
    }

    @Override
    public void validate(GraphModel model, List<Issue> issues) {
        check("name", model.getName(), this.MAX_NAME_LENGTH, "name", issues);
        check("description", model.getDescription(), this.MAX_MODEL_DESCRIPTION_LENGTH, "description", issues);
        for (var entry : model.getNodes().entrySet()) {
            var nodeId = entry.getKey();
            var node = entry.getValue();
            checkEntity("nodes." + nodeId, node.getDescription(), node.getProperties(), node.getConstraints(), node.getExtensions().getIndexes(), issues);
        }
        for (var entry : model.getRelationships().entrySet()) {
            var relationshipId = entry.getKey();
            var relationship = entry.getValue();
            checkEntity("relationships." + relationshipId, relationship.getDescription(), relationship.getProperties(), relationship.getConstraints(), relationship.getExtensions().getIndexes(), issues);
        }
    }

    private void checkEntity(String path, String description, Map<String, Property> properties,
            Map<String, ? extends Named> constraints, Map<String, ? extends Named> indexes,
            List<Issue> issues) {
        check("description", description, this.MAX_DESCRIPTION_LENGTH, path + ".description", issues);
        for (var entry : properties.entrySet()) {
            var propertyId = entry.getKey();
            var property = entry.getValue();
            String propertyPath = path + ".properties." + propertyId;
            check("name", property.getName(), this.MAX_NAME_LENGTH, propertyPath + ".name", issues);
            check("description", property.getDescription(), this.MAX_DESCRIPTION_LENGTH, propertyPath + ".description", issues);
        }
        for (var entry : constraints.entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            String constraintPath = path + ".constraints." + constraintId + ".name";
            check("name", constraint.getName(), this.MAX_CONSTRAINT_INDEX_NAME_LENGTH, constraintPath, issues);
        }
        for (var entry : indexes.entrySet()) {
            var indexId = entry.getKey();
            var index = entry.getValue();
            String indexPath = path + ".extensions.indexes." + indexId + ".name";
            check("name", index.getName(), this.MAX_CONSTRAINT_INDEX_NAME_LENGTH, indexPath, issues);
        }
    }

    private void check(String field, String value, int max, String path, List<Issue> issues) {
        if (value != null && value.length() > max) {
            issues.add(new Issue(field + "_too_long", "The " + field + " at '" + path + "' is " + value.length() + " characters long, the maximum is " + max, path, Kt.mapOf(new Pair<>("length", String.valueOf(value.length())), new Pair<>("max", String.valueOf(max)))));
        }
    }
}
