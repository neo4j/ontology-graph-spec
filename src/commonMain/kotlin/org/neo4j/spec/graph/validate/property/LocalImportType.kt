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
@file:JvmName("LocalImportTypes")

package org.neo4j.spec.graph.validate.property

import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.jvm.JvmName

// boolean, datetime, arrays of those, and the six vector types.
private val SUPPORTED_LOCAL_IMPORT_TYPES = setOf(
    Neo4jType.STRING,
    Neo4jType.INTEGER,
    Neo4jType.FLOAT,
    Neo4jType.BOOLEAN,
    Neo4jType.ZONED_DATETIME,
    Neo4jType.LIST_STRING,
    Neo4jType.LIST_INTEGER,
    Neo4jType.LIST_FLOAT,
    Neo4jType.LIST_BOOLEAN,
    Neo4jType.LIST_ZONED_DATETIME,
    Neo4jType.VECTOR_FLOAT,
    Neo4jType.VECTOR_FLOAT32,
    Neo4jType.VECTOR_INTEGER,
    Neo4jType.VECTOR_INTEGER32,
    Neo4jType.VECTOR_INTEGER16,
    Neo4jType.VECTOR_INTEGER8
)

fun unsupportedLocalImportType(propertyId: String, property: Property, entityPath: String): Issue? {
    if (property.type == Neo4jType.ANY) return null
    if (property.type !in SUPPORTED_LOCAL_IMPORT_TYPES) {
        return Issue(
            code = "unsupported_local_import_property_type",
            message = "Property '$propertyId' type '${Neo4jType.toString(property.type)}' is not supported " +
                "in local imports",
            path = "$entityPath.properties.$propertyId.type"
        )
    }
    return null
}
