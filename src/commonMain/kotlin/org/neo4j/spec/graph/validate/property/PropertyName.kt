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
@file:JvmName("PropertyNames")

package org.neo4j.spec.graph.validate.property

import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.jvm.JvmName

/**
 * Shared logic for empty and duplicate property name checks.
 * Used by both node and relationship property validators.
 *
 * UPX: `getPropertyErrors` (errors.ts#L112-L124), same logic for nodes and relationships.
 */
fun emptyPropertyName(propertyId: String, property: Property, entityPath: String): Issue? {
    val name = property.name
    if (name.isNullOrBlank()) {
        return Issue(
            code = "missing_property_name",
            message = "Missing name for property '$propertyId'",
            path = "$entityPath.properties.$propertyId.name"
        )
    }
    return null
}

fun duplicatePropertyName(
    properties: Map<String, Property>,
    propertyId: String,
    property: Property,
    entityPath: String
): Issue? {
    val name = property.name ?: return null
    if (name.isBlank()) return null
    // UPX findArrayDuplicates + includes flags ALL properties with a duplicated name
    val isDuplicate = properties.any { (otherId, other) ->
        otherId != propertyId && other.name == name
    }
    if (isDuplicate) {
        return Issue(
            code = "duplicate_property_name",
            message = "Duplicate property name '$name'",
            path = "$entityPath.properties.$propertyId.name"
        )
    }
    return null
}

/**
 * A property may restrict its values with either a `pattern` or a `one_of` list, but not both.
 */
fun patternOneOfConflict(propertyId: String, property: Property, entityPath: String): Issue? {
    if (property.pattern.isNotEmpty() && property.oneOf.isNotEmpty()) {
        return Issue(
            code = "property_pattern_one_of_conflict",
            message = "Property '$propertyId' can't set both pattern and one_of",
            path = "$entityPath.properties.$propertyId"
        )
    }
    return null
}
