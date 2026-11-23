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
package org.neo4j.spec.graph.validate.property;

import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.validate.Issue;

public final class PropertyNames {
    private PropertyNames() {}

    /**
     * Shared logic for empty and duplicate property name checks.
     * Used by both node and relationship property validators.
     *
     * UPX: `getPropertyErrors` (errors.ts#L112-L124), same logic for nodes and relationships.
     */
    public static Issue emptyPropertyName(String propertyId, Property property, String entityPath) {
        String name = property.getName();
        if (name == null || name.isBlank()) {
            return new Issue(
                    "missing_property_name",
                    "Missing name for property '" + propertyId + "'",
                    entityPath + Issue.jsonPointerOf("properties", propertyId, "name"));
        }
        return null;
    }

    public static Issue duplicatePropertyName(
            Map<String, Property> properties, String propertyId, Property property, String entityPath) {
        String name = property.getName();
        if (name == null) {
            return null;
        }
        if (name.isBlank()) {
            return null;
        }
        boolean isDuplicate = false;
        for (var entry : properties.entrySet()) {
            if (!Objects.equals(entry.getKey(), propertyId)
                    && Objects.equals(entry.getValue().getName(), name)) {
                isDuplicate = true;
                break;
            }
        }
        if (isDuplicate) {
            return new Issue(
                    "duplicate_property_name",
                    "Duplicate property name '" + name + "'",
                    entityPath + Issue.jsonPointerOf("properties", propertyId, "name"));
        }
        return null;
    }

    /**
     * A property may restrict its values with either a `pattern` or a `one_of` list, but not both.
     */
    public static Issue patternOneOfConflict(String propertyId, Property property, String entityPath) {
        if (!property.getPattern().isEmpty() && !property.getOneOf().isEmpty()) {
            return new Issue(
                    "property_pattern_one_of_conflict",
                    "Property '" + propertyId + "' can't set both pattern and one_of",
                    entityPath + Issue.jsonPointerOf("properties", propertyId));
        }
        return null;
    }
}
