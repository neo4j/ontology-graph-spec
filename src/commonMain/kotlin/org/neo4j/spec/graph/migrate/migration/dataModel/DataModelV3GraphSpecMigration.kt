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
package org.neo4j.spec.graph.migrate.migration.dataModel

import org.neo4j.spec.graph.codec.schema.SchemaElement
import org.neo4j.spec.graph.codec.schema.SchemaLiteral
import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.codec.schema.schemaMapOf
import org.neo4j.spec.graph.codec.schema.toNotEmpty
import org.neo4j.spec.graph.migrate.Migration
import org.neo4j.spec.graph.model.NameFormat
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import org.neo4j.spec.graph.model.extension.mapping.MappingType
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.model.type.ConstraintType.PROPERTY_TYPE
import org.neo4j.spec.graph.model.type.IndexType
/**
 * 3.0 -> Graph Spec 4.0
 */
class DataModelV3GraphSpecMigration :
    Migration(
        fromType = Type.DATA_MODEL,
        from = Version.DATA_MODEL_V3_0,
        toType = Type.GRAPH_SPEC,
        to = Version.LATEST
    ) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        val extensions = schema.mapOrNull("graphSchemaExtensionsRepresentation")
        val nodeKeys = keyProperties(extensions, "node")
        val relKeys = keyProperties(extensions, "relationship")
        // Support source-schema-only input via migrating just the tables. Nodes and relationships
        // are emitted as empty maps to match the full migration path of them always being present
        val graphSchema = schema.mapOrNull("graphSchemaRepresentation")?.mapOrNull("graphSchema")
            ?: return schemaMapOf(
                "schema" to Version.schemaUrl(Version.LATEST),
                "version" to "1.0.0",
                "id" to "graph-model",
                "description" to schema.literalOrNull("description"),
                "nodes" to emptyMap<String, SchemaMap>(),
                "relationships" to emptyMap<String, SchemaMap>(),
                "extensions" toNotEmpty extensions(migrateTables(schema), emptyList())
            )
        val (nodeConstraints, relationshipConstraints) = gatherWithNames(graphSchema, "constraints")
        val (nodeIndexes, relationshipIndexes) = gatherWithNames(graphSchema, "indexes")
        val nodes = migrateNodes(graphSchema, nodeConstraints, nodeIndexes)
        convertVisualisations(schema, nodes)
        return schemaMapOf(
            "schema" to Version.schemaUrl(Version.LATEST),
            "version" to "1.0.0",
            "id" to "graph-model",
            "description" to schema.literalOrNull("description"),
            "nodes" to nodes,
            "relationships" to migrateRelationships(graphSchema, relationshipConstraints, relationshipIndexes),
            "extensions" toNotEmpty extensions(
                migrateTables(schema),
                nodeMappings(schema, nodeKeys) + relationshipMappings(schema, relKeys)
            )
        )
    }

    /**
     * Graph model level extensions, omitting any which are empty.
     */
    private fun extensions(tables: Map<String, SchemaMap>, mappings: List<SchemaMap>): Map<String, Any> {
        val extensions = mutableMapOf<String, Any>()
        if (tables.isNotEmpty()) {
            extensions["data_source_tables"] = tables
        }
        if (mappings.isNotEmpty()) {
            extensions["data_source_mappings"] = mapOf(MAPPING_ID to mappings)
        }
        return extensions
    }

    private fun keyProperties(extensions: SchemaMap?, entity: String): Map<String, Set<String>> =
        extensions?.listOfMapsOrNull("${entity}KeyProperties")?.associate { kp ->
            kp.ref(entity) to kp.listOfMaps("keyProperties").map { it.ref() }.toSet()
        } ?: emptyMap()

    /**
     * Moves node positions into each node's extensions.
     */
    internal fun convertVisualisations(schema: SchemaMap, nodes: MutableMap<String, SchemaMap>) {
        val visualisation = schema.remove("visualisation") as? SchemaMap ?: return
        for (vis in visualisation.listOfMaps("nodes")) {
            val ref = vis.string("id").removePrefix("#")
            val node = nodes[ref] ?: error("Unknown node $ref")
            val position = vis.map("position")
            val extensions = node.mapOrNull("extensions") ?: schemaMapOf().also { node["extensions"] = it }
            extensions["display"] = schemaMapOf(
                "x" to position.literal("x"),
                "y" to position.literal("y")
            )
        }
    }

    internal fun gatherWithNames(
        schema: SchemaMap,
        key: String
    ): Pair<Map<String, List<SchemaMap>>, Map<String, List<SchemaMap>>> {
        val elements = schema.listOfMapsOrNull(key) ?: return Pair(emptyMap(), emptyMap())
        val (nodes, rels) = elements.partition { it.string("entityType") == "node" }
        // It's assumed that all nodes have nodeLabel fields and rels have relationshipType's
        val nodeElements = nodes.groupBy { it.ref("nodeLabel") }
        val relationshipElements = rels.groupBy { it.ref("relationshipType") }
        for (element in elements) {
            element["name"] = element.stringOrNull("name")
        }
        return Pair(nodeElements, relationshipElements)
    }

    internal fun migrateNodes(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>
    ): MutableMap<String, SchemaMap> {
        val nodes = mutableMapOf<String, SchemaMap>()
        val nodeLabels = schema.listOfMapsOrNull("nodeLabels")?.associateBy { it.id() } ?: return nodes
        val nodeObjTypes = schema.listOfMapsOrNull("nodeObjectTypes") ?: return nodes
        for (nodeObject in nodeObjTypes) {
            val labelRefs = nodeObject.listOfMaps("labels").map { it.ref() }
            val labels = labelRefs.map { labelRef ->
                nodeLabels[labelRef] ?: error("Label $labelRef not found")
            }
            val tokens = labels.map { it.string("token") }
            val labelRef = labelRefs.firstOrNull() // TODO loop all
            val primaryLabel = tokens.first()
            val propertyTokens = labels.flatMap { it.listOfMapsOrNull("properties").orEmpty() }
                .associate { it.id() to it.string("token") }
            val id = nodeObject.id()
            nodes[id] = schemaMapOf(
                "labels" to schemaMapOf(
                    "identifier" to primaryLabel,
                    "implied" toNotEmpty tokens.drop(1)
                    // TODO optional
                ),
                "constraints" toNotEmpty convertConstraints(constraints, labelRef, primaryLabel, propertyTokens),
                "properties" toNotEmpty convertProperties(labels),
                "extensions" toNotEmpty mapOf(
                    "indexes" to convertIndexes(indexes, labelRef, primaryLabel, propertyTokens)
                ).filterValues { !it.isNullOrEmpty() }.mapValues { it.value!! },
                "description" to nodeObject.literalOrNull("description")
            )
        }
        return nodes
    }

    internal fun convertIndexes(
        indexes: Map<String, List<SchemaMap>>,
        labelRef: String?,
        label: String,
        propertyTokens: Map<String, String>
    ): Map<String, SchemaMap>? = indexes[labelRef]?.associate { index ->
        val id = index.id()
        val resolvedType = indexType(index)
        val properties = index.listOfMapsOrNull("properties")
        id to schemaMapOf(
            "type" to resolvedType.name,
            "labels" to listOf(label),
            "properties" to properties?.map { it.ref() },
            "options" toNotEmpty index.mapOrNull("options")?.also { it["type"] = resolvedType.name },
            "name" to (
                index.stringOrNull("name")?.takeUnless { it.isBlank() }
                    ?: NameFormat.indexName(properties.tokensFrom(propertyTokens), label, resolvedType)
                )
        )
    }

    private fun indexType(index: SchemaMap): IndexType {
        val type = index.string("indexType")
        return dataModelIndexTypeFrom(type)
            ?: error("Unknown index type: '$type' at ${index.path}.${index.string("name")}")
    }

    internal fun convertConstraints(
        constraints: Map<String, List<SchemaMap>>,
        labelRef: String?,
        label: String,
        propertyTokens: Map<String, String>
    ): Map<String, SchemaMap>? = constraints[labelRef]?.associate { constraint ->
        val properties = constraint.listOfMapsOrNull("properties")
        val constraintType = constraintType(constraint)
        if (properties != null && properties.size > 1 && constraintType == PROPERTY_TYPE) {
            error("Type constraints not supported on multiple properties.")
        }
        val id = constraint.id()
        id to schemaMapOf(
            "type" to constraintType.name,
            "properties" toNotEmpty properties?.map { it.ref() },
            "name" to (
                constraint.stringOrNull("name")?.takeUnless { it.isBlank() }
                    ?: generatedName(properties, propertyTokens, label, constraintType)
                )
        )
    }

    private fun generatedName(
        properties: List<SchemaMap>?,
        propertyTokens: Map<String, String>,
        label: String,
        constraintType: ConstraintType
    ): String = NameFormat.constraintName(
        properties.tokensFrom(propertyTokens),
        label,
        constraintType
    )

    private fun List<SchemaMap>?.tokensFrom(propertyTokens: Map<String, String>): List<String> =
        orEmpty().map { propertyTokens[it.ref()] ?: it.ref() }

    private fun constraintType(constraint: SchemaMap): ConstraintType {
        val type = constraint.string("constraintType")
        return dataModelConstraintTypeFrom(type)
            ?: error("Unknown constraint type: $type at ${constraint.path}.${constraint.string("name")}")
    }

    internal fun migrateRelationships(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>
    ): MutableMap<String, SchemaMap> {
        val relationships = mutableMapOf<String, SchemaMap>()
        val relationshipTypes =
            schema.listOfMapsOrNull("relationshipTypes")?.associateBy { it.id() } ?: return relationships
        val relObjTypes = schema.listOfMapsOrNull("relationshipObjectTypes") ?: return relationships
        for (objectType in relObjTypes) {
            val typeRef = objectType.ref("type")
            val relationshipType = relationshipTypes[typeRef] ?: error("RelationshipType $typeRef not found")
            val token = relationshipType.string("token")
            val propertyTokens = relationshipType.listOfMapsOrNull("properties").orEmpty()
                .associate { it.id() to it.string("token") }
            val id = objectType.id()
            relationships[id] = schemaMapOf(
                "type" to token,
                "from" to mapOf("node" to objectType.ref("from")),
                "to" to mapOf("node" to objectType.ref("to")),
                "properties" to convertProperties(listOf(relationshipType)),
                "constraints" toNotEmpty convertConstraints(constraints, typeRef, token, propertyTokens),
                "extensions" toNotEmpty mapOf(
                    "indexes" to convertIndexes(indexes, typeRef, token, propertyTokens)
                ).filterValues { !it.isNullOrEmpty() }.mapValues { it.value!! },
                "description" to objectType.literalOrNull("description")
            )
        }
        return relationships
    }

    internal fun convertProperties(labels: List<SchemaMap>): Map<String, SchemaMap> = labels
        .flatMap { label -> label.listOfMaps("properties") }
        .associate { property ->
            val typeObj = property.map("type")
            val map = schemaMapOf(
                "name" to property.literalOrNull("token"),
                "type" to neo4jType(typeObj),
                "dimension" to dimension(typeObj),
                "description" to property.literalOrNull("description")
            )
            property.id() to map
        }

    internal fun relationshipMappings(schema: SchemaMap, relKeys: Map<String, Set<String>>): List<SchemaMap> {
        val graph = schema.map("graphSchemaRepresentation").map("graphSchema")
        val relInfo = graph.listOfMapsOrNull("relationshipObjectTypes")?.associateBy { it.id() } ?: return emptyList()
        val relationshipMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("relationshipMappings") ?: return emptyList()
        val mappings = mutableListOf<SchemaMap>()
        for (mapping in relationshipMappings) {
            val ref = mapping.ref("relationship")
            if (ref !in relInfo) error("Relationship $ref not found")
            mappings += schemaMapOf(
                "type" to SchemaLiteral(MappingType.RELATIONSHIP), // needed for Kotlin/Native migrations
                "relationship" to ref,
                "from_node" to mapOf("properties" to mapping.entityMap("fromMappings")),
                "to_node" to mapOf("properties" to mapping.entityMap("toMappings")),
                "table" to mapping.literal("tableName"),
                "properties" toNotEmpty migratePropertyMappings(mapping),
                "key" toNotEmpty migrateKeyProperties(mapping, relKeys[ref] ?: emptySet())
            )
        }
        return mappings
    }

    internal fun SchemaMap.entityMap(key: String) = mapOrNull(key)?.map { (key, value) ->
        key.removePrefix("#") to schemaMapOf("column" to value)
    }?.toMap() ?: emptyMap()

    internal fun nodeMappings(schema: SchemaMap, nodeKeys: Map<String, Set<String>>): List<SchemaMap> {
        val nodeMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("nodeMappings") ?: return emptyList()
        val mappings = mutableListOf<SchemaMap>()
        for (nodeMapping in nodeMappings) {
            val ref = nodeMapping.ref("node")
            mappings += schemaMapOf(
                "type" to SchemaLiteral(MappingType.NODE), // needed for Kotlin/Native migrations
                "node" to ref,
                "table" to nodeMapping.literal("tableName"),
                "properties" toNotEmpty migratePropertyMappings(nodeMapping),
                "key" toNotEmpty migrateKeyProperties(nodeMapping, nodeKeys[ref] ?: emptySet())
            )
        }
        return mappings
    }

    private fun migratePropertyMappings(entity: SchemaMap) = entity
        .listOfMaps("propertyMappings")
        .associate { mapping ->
            mapping.ref("property") to mapOf(
                "column" to mapping.literal("fieldName")
            )
        }

    private fun migrateKeyProperties(entity: SchemaMap, keys: Set<String>) = entity
        .listOfMaps("propertyMappings")
        .mapNotNull { mapping ->
            val ref = mapping.ref("property")
            if (keys.contains(ref)) ref else null
        }

    internal fun migrateTables(schema: SchemaMap): MutableMap<String, SchemaMap> {
        val tables = mutableMapOf<String, SchemaMap>()
        val sourceSchema = schema.map("graphMappingRepresentation").mapOrNull("dataSourceSchema") ?: return tables
        for (table in sourceSchema.listOfMaps("tableSchemas")) {
            val name = table.string("name")
            tables[name] = schemaMapOf(
                "source" to sourceSchema.literalOrNull("type"),
                "columns" to migrateColumns(table),
                "primary_keys" toNotEmpty table.listOrNull("primaryKeys"),
                "foreign_keys" toNotEmpty migrateForeignKeys(table)
            )
        }
        return tables
    }

    private fun migrateForeignKeys(table: SchemaMap): MutableMap<String, SchemaElement>? {
        val foreign = table.listOfMapsOrNull("foreignKeys") ?: return null
        val foreignKeys = mutableMapOf<String, SchemaElement>()
        for (foreignKey in foreign) {
            val columns = foreignKey.listOfMaps("fields").map { it.string("field") }
            val referencedColumns = foreignKey.listOfMaps("fields").map { it.string("referencedField") }
            foreignKeys["foreignKey${foreignKeys.size + 1}"] = schemaMapOf(
                "columns" to columns,
                "references" to mapOf(
                    "table" to foreignKey.literal("referencedTable"),
                    "columns" to referencedColumns
                )
            )
        }
        return foreignKeys
    }

    private fun migrateColumns(table: SchemaMap): List<SchemaMap> = table.listOfMaps("fields").map { field ->
        schemaMapOf(
            "name" to field.literal("name"),
            "type" to field.literalOrNull("rawType"),
            "size" to field.literalOrNull("size"),
            "suggested" to neo4jType(field.mapOrNull("recommendedType")),
            "supported" to field.listOfMapsOrNull("supportedTypes")?.map {
                neo4jType(it)
            },
            "dimension" to (
                dimension(field.mapOrNull("recommendedType"))
                    ?: field.listOfMapsOrNull("supportedTypes")?.firstNotNullOfOrNull { dimension(it) }
                ),
            "custom" toNotEmpty field.literalOrNull("sample")?.let { mapOf("sample" to it) }
        )
    }

    companion object {
        const val MAPPING_ID = "mapping0"

        private fun neo4jType(type: SchemaMap?): String? {
            val base = type?.stringOrNull("type")?.lowercase() ?: return null
            return when (base) {
                "array" -> itemType(type)?.let { "LIST<$it>" }
                "vector" -> itemType(type)?.let { "VECTOR<$it>" }
                else -> scalarType(base)
            }
        }

        private fun dimension(type: SchemaMap?): Int? =
            if (type?.stringOrNull("type")?.lowercase() == "vector") type.intOrNull("dimension") else null

        private fun itemType(type: SchemaMap): String? = scalarType(type.mapOrNull("items")?.stringOrNull("type"))

        private fun scalarType(string: String?): String? = when (val lower = string?.lowercase()) {
            null -> null
            // spaced and renamed types are not a simple uppercasing
            "localdatetime" -> "LOCAL DATETIME"
            "datetime" -> "ZONED DATETIME"
            "localtime" -> "LOCAL TIME"
            "time" -> "ZONED TIME"
            // everything else (string, integer, float, boolean, point, date, duration
            // and the vector coordinate variants like float32) is just its uppercase name
            else -> lower.uppercase()
        }
    }
}
