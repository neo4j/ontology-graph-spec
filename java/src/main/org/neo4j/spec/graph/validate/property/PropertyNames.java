// Generated from org/neo4j/spec/graph/validate/property/PropertyName.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.property;

import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.validate.Issue;

public final class PropertyNames {
    private PropertyNames() {
    }

    /**
     * Shared logic for empty and duplicate property name checks.
     * Used by both node and relationship property validators.
     *
     * UPX: `getPropertyErrors` (errors.ts#L112-L124), same logic for nodes and relationships.
     */
    public static Issue emptyPropertyName(String propertyId, Property property, String entityPath) {
        String name = property.getName();
        if (name == null || name.isBlank()) {
            return new Issue("missing_property_name", "Missing name for property '" + propertyId + "'", entityPath + ".properties." + propertyId + ".name");
        }
        return null;
    }

    public static Issue duplicatePropertyName(Map<String, Property> properties, String propertyId,
            Property property, String entityPath) {
        String name = property.getName();
        if (name == null) {
            return null;
        }
        if (name.isBlank()) {
            return null;
        }
        boolean isDuplicate = false;
        for (var entry : properties.entrySet()) {
            if (!Objects.equals(entry.getKey(), propertyId) && Objects.equals(entry.getValue().getName(), name)) {
                isDuplicate = true;
                break;
            }
        }
        if (isDuplicate) {
            return new Issue("duplicate_property_name", "Duplicate property name '" + name + "'", entityPath + ".properties." + propertyId + ".name");
        }
        return null;
    }

    /**
     * A property may restrict its values with either a `pattern` or a `one_of` list, but not both.
     */
    public static Issue patternOneOfConflict(String propertyId, Property property,
            String entityPath) {
        if (!property.getPattern().isEmpty() && !property.getOneOf().isEmpty()) {
            return new Issue("property_pattern_one_of_conflict", "Property '" + propertyId + "' can't set both pattern and one_of", entityPath + ".properties." + propertyId);
        }
        return null;
    }
}
