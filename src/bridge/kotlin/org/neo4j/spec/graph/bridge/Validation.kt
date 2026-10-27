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
package org.neo4j.spec.graph.bridge

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.validate.Validation
import org.neo4j.spec.graph.validate.ValidationTree
import org.neo4j.spec.graph.validate.Validations
import kotlin.experimental.ExperimentalNativeApi

private val jsonFormat = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
}

private val validationLabelsToSet: Map<String, List<Validation>> = mapOf(
    "core" to Validations.core,
    "labelled" to Validations.labelled,
    "consistent_schema" to Validations.consistentSchema,
    "mappable" to Validations.mappable,
    "well_constrained" to Validations.wellConstrained,
    "all" to Validations.all
)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("validate")
fun validate(
    inputJson: CPointer<ByteVar>?,
    validationSets: CPointer<ByteVar>?,
    outputBuffer: CPointer<ByteVar>?,
    bufferSize: Int
) = invokeBridge(
    inputJson,
    validationSets,
    outputBuffer = outputBuffer,
    bufferSize = bufferSize
) { input -> validateContent(content = input[0], sets = input[1]) }

internal fun validateContent(content: String, sets: String): String {
    val validators = sets.split(",").flatMap { label ->
        validationLabelsToSet.getOrElse(label) {
            throw IllegalArgumentException("unknown validation type: $label")
        }
    }.distinct()
    val validation = ValidationTree().also { it.build(validators) }

    val graphModel = GraphSpec.Json.decodeFromString(content)
    val issues = validation.validate(graphModel)
    return jsonFormat.encodeToString(issues)
}
