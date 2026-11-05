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
package org.neo4j.spec.graph.model.extension.custom

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

// Maximum nesting in custom values to guard against stack overflows
internal const val MAX_CUSTOM_DEPTH = 128

/**
 * Serializes an untyped custom extension value with an implicit type:
 * a [String], [Boolean], [Long], [Double], [List], [Set] or [Map] of string keys, nested arbitrarily.
 * A [Set] is encoded as a list and decodes as a [List].
 *
 * Integral numbers decode as [Long] and decimals as [Double].
 */
object AnyValueSerializer : KSerializer<Any> {
    // Named "kotlin.Any" so the JSON schema generator emits an unconstrained schema `{}`
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("kotlin.Any", SerialKind.CONTEXTUAL)

    private val listSerializer by lazy { ListSerializer(this) }
    private val mapSerializer by lazy { MapSerializer(String.serializer(), this) }

    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is String -> encoder.encodeString(value)
            is Boolean -> encoder.encodeBoolean(value)
            is Long -> encoder.encodeLong(value)
            // Checked before other numbers as on Kotlin/JS all numbers pass `is Int`
            is Double, is Float -> encoder.encodeDouble((value as Number).toDouble())
            is Number -> encoder.encodeLong(value.toLong())
            // Checked before lists and maps as JsonArray and JsonObject are also a List and Map
            is JsonElement -> throw unsupported(value)
            is List<*> -> encoder.encodeSerializableValue(listSerializer, value.map { it.requireValue() })
            is Set<*> -> encoder.encodeSerializableValue(listSerializer, value.map { it.requireValue() })
            is Map<*, *> -> encoder.encodeSerializableValue(
                mapSerializer,
                value.entries.associate { (key, value) ->
                    (key as? String ?: throw SerializationException("Custom map keys must be strings, found: $key")) to
                        value.requireValue()
                }
            )
            else -> throw unsupported(value)
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        val json = decoder as? JsonDecoder
            ?: throw SerializationException("Custom values can only be decoded by a JSON decoder")
        return json.decodeJsonElement().toAnyValue()
    }

    private fun unsupported(value: Any) = SerializationException(
        "Unsupported custom value type: ${value::class.simpleName}. " +
            "Use setSerializable(key, value) to store @Serializable objects, " +
            "otherwise only String, Boolean, Long, Double, List, Set and Map values are supported."
    )

    private fun Any?.requireValue(): Any = this ?: throw SerializationException("Custom values cannot be null")

    internal fun JsonElement.toAnyValue(depth: Int = 1): Any = when (this) {
        JsonNull -> throw SerializationException("Custom values cannot be null")
        is JsonPrimitive -> if (isString) {
            content
        } else {
            // Parse the content directly as JsonPrimitive.longOrNull truncates decimals on Kotlin/JS
            booleanOrNull ?: content.toLongOrNull() ?: content.toDoubleOrNull()
                ?: throw SerializationException("Unsupported custom value: $content")
        }
        is JsonArray -> {
            requireDepth(depth)
            mapTo(mutableListOf()) { it.toAnyValue(depth + 1) }
        }
        is JsonObject -> {
            requireDepth(depth)
            mapValuesTo(mutableMapOf()) { (_, value) -> value.toAnyValue(depth + 1) }
        }
    }

    /** Fails if [depth] levels of nested lists and maps is more than [MAX_CUSTOM_DEPTH]. */
    internal fun requireDepth(depth: Int) {
        if (depth > MAX_CUSTOM_DEPTH) {
            throw SerializationException("Custom values cannot be nested more than $MAX_CUSTOM_DEPTH levels deep")
        }
    }

    internal fun Any?.checkDepth(depth: Int = 1) {
        when (this) {
            is List<*> -> {
                requireDepth(depth)
                forEach { it.checkDepth(depth + 1) }
            }
            is Set<*> -> {
                requireDepth(depth)
                forEach { it.checkDepth(depth + 1) }
            }
            is Map<*, *> -> {
                requireDepth(depth)
                values.forEach { it.checkDepth(depth + 1) }
            }
        }
    }
}
