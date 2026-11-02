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
@file:JvmName("GraphSpecs")
@file:JvmMultifileClass

package org.neo4j.spec.graph

import kotlinx.serialization.json.JsonBuilder
import java.util.function.Consumer

/**
 * Java-friendly form of the [GraphSpec] builder function, taking a [Consumer] rather than a Kotlin lambda so Java
 * callers don't have to return `Unit.INSTANCE`.
 */
@JvmOverloads
fun graphSpec(from: GraphSpec = GraphSpec.Json, configure: Consumer<GraphSpecConfig.Builder>): GraphSpec =
    GraphSpec(from) { configure.accept(this) }

/**
 * Java-friendly form of [GraphSpecConfig.Builder.json], taking a [Consumer] rather than a Kotlin lambda.
 */
fun GraphSpecConfig.Builder.json(configure: Consumer<JsonBuilder>) {
    json { configure.accept(this) }
}
