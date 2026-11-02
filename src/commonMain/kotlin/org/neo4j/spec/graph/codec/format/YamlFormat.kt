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
package org.neo4j.spec.graph.codec.format

import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlElement
import net.mamoe.yamlkt.YamlList
import net.mamoe.yamlkt.YamlLiteral
import net.mamoe.yamlkt.YamlMap
import net.mamoe.yamlkt.YamlNull
import org.neo4j.spec.graph.codec.schema.SchemaElement
import org.neo4j.spec.graph.codec.schema.SchemaList
import org.neo4j.spec.graph.codec.schema.SchemaLiteral
import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.codec.schema.SchemaNull
import org.neo4j.spec.graph.model.GraphModel
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.jvm.JvmStatic

class YamlFormat(private val yaml: Yaml, private val json: JsonFormat, options: YamlPrintOptions) : Format {

    // The YAML library isn't as customisable as we'd like so we're rolling our own writing
    private val writer = YamlWriter(options)

    override fun encodeToString(element: SchemaElement) = writer.write(element)

    private val regex = Regex(": (-[0-9.]+)")

    override fun decodeFromString(string: String): SchemaElement {
        /*
            The current YAML library fails to parse negative numbers correctly i.e. `: -100`
            Becomes [100] instead of -100.
            Luckily strings get auto interpreted as doubles/integers so we can do a basic find and replace to wrap
            quotation marks around all negative numbers in maps to make it processable.
            This is likely to miss edge-cases especially regarding extensions.
            This is a short-term hack as the library is no longer maintained so longer term either:
              1. We'll need to fork and maintain ourselves
              2. Write our own multi-platform yaml library
              3. Drop yaml support
         */
        val amended = markQuotedScalars(string).replace(regex, ": \"$1\"")
        val yaml = yaml.decodeYamlFromString(amended)
        return schemaElement(yaml)
    }

    override fun encodeToSchema(model: GraphModel) = json.encodeToSchema(model)

    /** [Yaml] library doesn't have proper AST -> Generic type support so we fall back to [json] */
    override fun decodeFromSchema(element: SchemaElement) = json.decodeFromSchema(element)

    fun schemaElement(yaml: YamlElement, parent: String = ""): SchemaElement = when (yaml) {
        is YamlList -> SchemaList(yaml.mapTo(mutableListOf()) { schemaElement(it) }, parent)
        is YamlMap -> SchemaMap(
            yaml.content.entries.associateTo(mutableMapOf()) { (key, value) ->
                if (key !is YamlLiteral) {
                    error("Failed to parse yaml: non-string key not supported: $key")
                }
                key.content.removePrefix(QUOTED_MARKER) to schemaElement(value)
            },
            parent
        )
        is YamlLiteral -> scalar(yaml.content, parent)
        YamlNull -> SchemaNull(parent)
    }

    /**
     * Quoted scalars are always strings, unquoted scalars are only literals when they're a boolean or a number.
     */
    private fun scalar(content: String, parent: String): SchemaLiteral = when {
        content.startsWith(QUOTED_MARKER) -> SchemaLiteral(content.removePrefix(QUOTED_MARKER), parent, isString = true)
        content == "true" || content == "false" || number.matches(content) ->
            SchemaLiteral(content, parent, isString = false)
        else -> SchemaLiteral(content, parent, isString = true)
    }

    private val number = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][-+]?[0-9]+)?")

    companion object {
        @JvmStatic
        val default = YamlFormat(
            Yaml {
                encodeDefaultValues = false
            },
            JsonFormat.default,
            YamlPrintOptions(
                alwaysQuoteStrings = true,
                inlinePaths = setOf(
                    "nodes.*.properties.*",
                    "nodes.*.constraints.*.properties",
                    "nodes.*.extensions.indexes.*.labels",
                    "nodes.*.extensions.indexes.*.properties",
                    "relationships.*.properties.*",
                    "relationships.*.from",
                    "relationships.*.to",
                    "relationships.*.constraints.*.properties",
                    "relationships.*.extensions.indexes.*.properties",
                    "relationships.*.extensions.indexes.*.options.*",
                    "extensions.data_source_tables.*.columns[*].supported",
                    "extensions.data_source_tables.*.primary_keys",
                    "extensions.data_source_tables.*.foreign_keys.*.columns",
                    "extensions.data_source_tables.*.foreign_keys.*.references.columns",
                    "extensions.data_source_mappings.*[*].properties.*",
                    "extensions.data_source_mappings.*[*].key",
                    "extensions.data_source_mappings.*[*].from_node.properties.*",
                    "extensions.data_source_mappings.*[*].to_node.properties.*"
                )
            )
        )
    }
}
