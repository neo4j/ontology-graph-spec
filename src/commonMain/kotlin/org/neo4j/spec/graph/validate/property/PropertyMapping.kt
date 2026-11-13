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
@file:JvmName("PropertyMappings")

package org.neo4j.spec.graph.validate.property

import org.neo4j.spec.graph.validate.Issue
import kotlin.jvm.JvmName

fun missingPropertyMapping(propertyId: String, entityPath: String): Issue = Issue(
    code = "missing_property_mapping",
    message = "Property '$propertyId' is not mapped to any source column",
    path = "$entityPath.properties.$propertyId"
)
