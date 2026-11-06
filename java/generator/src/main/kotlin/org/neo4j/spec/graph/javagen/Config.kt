package org.neo4j.spec.graph.javagen

/**
 * What the generator skips or leaves to hand-written Java in java/library/src/main/java.
 *
 * Kept here rather than as annotations in the Kotlin sources so the existing distributions are untouched.
 */
object Config {
    /** Source files (relative to the source root) that aren't translated at all. */
    val excludedFiles = setOf(
        // kotlinx-serialization specific, replaced by Jackson in the hand-written runtime
        "org/neo4j/spec/graph/codec/format/JsonFormat.kt",
        "org/neo4j/spec/graph/model/extension/custom/AnyValueSerializer.kt",
        "org/neo4j/spec/graph/model/extension/custom/CustomMapSerializer.kt",
        "org/neo4j/spec/graph/model/extension/custom/SerializableValues.kt",
        "org/neo4j/spec/graph/schema/Description.kt",
        "org/neo4j/spec/graph/GraphSpecConfig.kt",
        // JSON only for now
        "org/neo4j/spec/graph/codec/format/YamlFormat.kt",
        "org/neo4j/spec/graph/codec/format/YamlWriter.kt",
        "org/neo4j/spec/graph/codec/format/YamlQuotes.kt",
        "org/neo4j/spec/graph/codec/format/YamlPrintOptions.kt",
    )

    /** Fully qualified declarations that aren't translated, e.g. members tied to kotlinx or YAML. */
    val excludedDeclarations = setOf(
        "org.neo4j.spec.graph.GraphSpec.Yaml",
        "org.neo4j.spec.graph.GraphSpec.Companion.yaml",
        "org.neo4j.spec.graph.model.extension.CustomExtensions.setSerializable",
        "org.neo4j.spec.graph.model.extension.CustomExtensions.getSerializable",
        "org.neo4j.spec.graph.model.extension.setSerializable",
        "org.neo4j.spec.graph.model.extension.getSerializable",
    )

    /** Kotlin names that are Java keywords or otherwise unusable get renamed. */
    val renames = mapOf(
        "org.neo4j.spec.graph.codec.schema.SchemaMap.int" to "integer",
        "org.neo4j.spec.graph.codec.schema.SchemaMap.intOrNull" to "integerOrNull",
    )

    /** Package of the hand-written runtime helpers the generated code calls. */
    const val RUNTIME_PACKAGE = "org.neo4j.spec.graph.internal"
}
