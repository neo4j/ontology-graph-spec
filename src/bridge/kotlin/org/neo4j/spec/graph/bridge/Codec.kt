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
import org.neo4j.spec.graph.model.GraphModel
import kotlin.experimental.ExperimentalNativeApi

// Transport carries effective defaults to foreign language models. Public document formatting stays in GraphSpec.
private val transport = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

private fun codec(format: String): GraphSpec = when (format) {
    "json" -> GraphSpec.Json
    "yaml" -> GraphSpec.Yaml
    else -> throw IllegalArgumentException("unknown document format: $format")
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("graphspec_decode")
fun decode(
    content: CPointer<ByteVar>?,
    format: CPointer<ByteVar>?,
    sourceType: CPointer<ByteVar>?,
    outputBuffer: CPointer<ByteVar>?,
    bufferSize: Int
) = invokeBridge(content, format, sourceType, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
    decodeContent(input[0], input[1], input[2])
}

internal fun decodeContent(content: String, format: String, sourceType: String): String =
    transport.encodeToString(codec(format).decodeFromString(content, sourceType))

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("graphspec_encode")
fun encode(
    modelJson: CPointer<ByteVar>?,
    format: CPointer<ByteVar>?,
    targetType: CPointer<ByteVar>?,
    targetVersion: CPointer<ByteVar>?,
    outputBuffer: CPointer<ByteVar>?,
    bufferSize: Int
) = invokeBridge(
    modelJson,
    format,
    targetType,
    targetVersion,
    outputBuffer = outputBuffer,
    bufferSize = bufferSize
) { input -> encodeContent(input[0], input[1], input[2], input[3]) }

internal fun encodeContent(content: String, format: String, targetType: String, targetVersion: String): String =
    codec(format).encodeToString(transport.decodeFromString<GraphModel>(content), targetType, targetVersion)
