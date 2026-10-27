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
package org.neo4j.spec.graph.model.property

/**
 * **Experimental:** these JavaScript helpers (the `*Editor` classes and the plain `*Js` objects) are experimental
 * and may change or be removed in any release without notice.
 */
@JsExport
class PropertyEditor {
    companion object {

        @JsStatic
        fun setType(property: PropertyJs, type: String) { // TODO neo4jtype
            property.type = type
        }

        @JsStatic
        fun setDimension(property: PropertyJs, dimension: Int?) {
            property.dimension = dimension
        }

        @JsStatic
        fun setMustExist(property: PropertyJs, mustExist: Boolean) {
            property.mustExist = mustExist
            property.key = null
        }

        @JsStatic
        fun setUnique(property: PropertyJs, unique: Boolean) {
            property.unique = unique
            property.key = null
        }

        @JsStatic
        fun setKey(property: PropertyJs, key: Boolean) {
            property.key = key
            property.unique = null
            property.mustExist = null
        }

        @JsStatic
        fun setReference(property: PropertyJs, reference: String) {
            property.reference = reference
        }

        @JsStatic
        fun addAlias(property: PropertyJs, alias: String) {
            if (!property.aliases.contains(alias)) {
                property.aliases += alias
            }
        }

        @JsStatic
        fun removeAlias(property: PropertyJs, alias: String) {
            property.aliases = property.aliases.filter { it != alias }.toTypedArray()
        }

        @JsStatic
        fun setPattern(property: PropertyJs, pattern: String) {
            property.pattern = pattern
        }

        @JsStatic
        fun addOneOf(property: PropertyJs, value: String) {
            property.oneOf += value
        }

        @JsStatic
        fun removeOneOf(property: PropertyJs, index: Int) {
            if (index in property.oneOf.indices) {
                property.oneOf = property.oneOf.filterIndexed { i, _ -> i != index }.toTypedArray()
            }
        }

        @JsStatic
        fun setName(property: PropertyJs, name: String) {
            property.name = name
        }
    }
}
