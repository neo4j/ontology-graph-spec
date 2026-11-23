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
package org.neo4j.spec.graph.codec.schema;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SchemaMap extends AbstractMap<String, SchemaElement> implements SchemaElement {
    private final Map<String, SchemaElement> content;

    private SchemaElement parent;

    private String identifier;

    public SchemaMap(Map<String, ? extends SchemaElement> content) {
        this((Map<String, SchemaElement>) content, "");
    }

    public SchemaMap(Map<String, SchemaElement> content, String path) {
        this.content = content;
        this.parent = null;
        this.identifier = path;
        for (var entry : getContent().entrySet()) {
            var key = entry.getKey();
            var element = entry.getValue();
            SchemaElements.adopt(this, element, key);
        }
    }

    public SchemaMap() {
        this(new LinkedHashMap<>(), "");
    }

    @Override
    public Set<Map.Entry<String, SchemaElement>> entrySet() {
        return this.content.entrySet();
    }

    @Override
    public int size() {
        return this.content.size();
    }

    @Override
    public SchemaElement get(Object key) {
        return this.content.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return this.content.containsKey(key);
    }

    @Override
    public Set<String> keySet() {
        return this.content.keySet();
    }

    @Override
    public Collection<SchemaElement> values() {
        return this.content.values();
    }

    public Map<String, SchemaElement> getContent() {
        return this.content;
    }

    @Override
    public SchemaElement getParent() {
        return this.parent;
    }

    public void setParent(SchemaElement value) {
        this.parent = value;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String value) {
        this.identifier = value;
    }

    @Override
    public SchemaElement repath(String newPath) {
        return SchemaElements.reroot(this, newPath);
    }

    @Override
    public boolean equals(Object other) {
        return Objects.equals(getContent(), other);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getContent());
    }

    @Override
    public String toString() {
        return getContent().entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    public void set(String key, Object value) {
        getContent().put(key, SchemaElements.adopt(this, SchemaElements.toSchemaElement(value), key));
    }

    public SchemaElement remove(String key) {
        return getContent().remove(key);
    }

    public SchemaMap removeMap(String key) {
        SchemaElement map = getContent().remove(key);
        if (map == null) {
            throw new IllegalStateException("Expected map, found nothing at " + path(key));
        }
        SchemaMap value = map instanceof SchemaMap ? (SchemaMap) map : null;
        if (value == null) {
            throw new IllegalStateException(
                    "Expected map, found invalid type " + map.getClass().getSimpleName() + " at " + path(key));
        }
        return value;
    }

    public SchemaElement put(String key, SchemaElement value) {
        return getContent().put(key, SchemaElements.adopt(this, value, key));
    }

    public void putAll(Map<? extends String, ? extends SchemaElement> from) {
        for (var entry : from.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            put(key, value);
        }
    }

    public SchemaList list(String key) {
        SchemaElement list = getContent().get(key);
        if (list == null) {
            throw new IllegalStateException("Missing required list at " + path(key));
        }
        SchemaList value = list instanceof SchemaList ? (SchemaList) list : null;
        if (value == null) {
            throw new IllegalStateException(
                    "Expected list, found invalid type " + list.getClass().getSimpleName() + " at " + path(key));
        }
        return value;
    }

    public SchemaList listOrNull(String key) {
        SchemaElement receiver = getContent().get(key);
        return receiver == null ? null : receiver instanceof SchemaList ? (SchemaList) receiver : null;
    }

    public SchemaMap map(String key) {
        SchemaElement map = getContent().get(key);
        if (map == null) {
            throw new IllegalStateException("Missing required map at " + path(key));
        }
        SchemaMap value = map instanceof SchemaMap ? (SchemaMap) map : null;
        if (value == null) {
            throw new IllegalStateException(
                    "Expected map, found invalid type " + map.getClass().getSimpleName() + " at " + path(key));
        }
        return value;
    }

    public SchemaMap mapOrNull(String key) {
        SchemaElement receiver = getContent().get(key);
        return receiver == null ? null : receiver instanceof SchemaMap ? (SchemaMap) receiver : null;
    }

    public SchemaMap mapOrPut(String key) {
        SchemaElement existing = getContent().get(key);
        if (existing == null) {
            existing = SchemaElements.adopt(this, new SchemaMap(), key);
            getContent().put(key, existing);
        }
        return (SchemaMap) existing;
    }

    public SchemaPrimitive literal(String key) {
        SchemaElement literal = getContent().get(key);
        if (literal == null) {
            throw new IllegalStateException("Missing required literal at " + path(key));
        }
        SchemaPrimitive value = literal instanceof SchemaPrimitive ? (SchemaPrimitive) literal : null;
        if (value == null) {
            throw new IllegalStateException(
                    "Expected literal, found invalid type " + literal.getClass().getSimpleName() + " at " + path(key));
        }
        return value;
    }

    public SchemaLiteral literalOrNull(String key) {
        SchemaElement receiver = getContent().get(key);
        return receiver == null ? null : receiver instanceof SchemaLiteral ? (SchemaLiteral) receiver : null;
    }

    public boolean bool(String key) {
        String value = string(key);
        if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("The string doesn't represent a boolean value: " + value);
    }

    public Boolean boolOrNull(String key) {
        String receiver = stringOrNull(key);
        if ("true".equals(receiver)) {
            return Boolean.TRUE;
        } else if ("false".equals(receiver)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public String string(String key) {
        return literal(key).getString();
    }

    public String stringOrNull(String key) {
        SchemaLiteral receiver = literalOrNull(key);
        return receiver == null ? null : receiver.getString();
    }

    public Integer integer(String key) {
        String receiver = stringOrNull(key);
        return receiver == null ? null : Integer.parseInt(receiver);
    }

    public Integer integerOrNull(String key) {
        String receiver = stringOrNull(key);
        if (receiver == null || receiver.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(receiver);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Map<String, SchemaMap> mapOfMaps(String key) {
        SchemaElement map = getContent().get(key);
        if (map == null) {
            throw new IllegalStateException("Missing required map at " + path(key));
        }
        SchemaMap schemaMap = map instanceof SchemaMap ? (SchemaMap) map : null;
        if (schemaMap == null) {
            throw new IllegalStateException(
                    "Expected map, found invalid type " + map.getClass().getSimpleName() + " at " + path(key));
        }
        Map<String, SchemaMap> result = new LinkedHashMap<>();
        for (Map.Entry<String, SchemaElement> entry : schemaMap.getContent().entrySet()) {
            String key2 = entry.getKey();
            SchemaElement element = entry.getValue();
            SchemaMap child = element instanceof SchemaMap ? (SchemaMap) element : null;
            if (child == null) {
                throw new IllegalStateException("Expected map, found invalid type "
                        + ((SchemaMap) map).getClass().getSimpleName() + " at " + path(key2));
            }
            result.put(key2, child);
        }
        return result;
    }

    public Map<String, SchemaMap> mapOfMapsOrNull(String key) {
        Map<String, SchemaMap> map = new LinkedHashMap<>();
        SchemaMap receiver = mapOrNull(key);
        Map<String, SchemaElement> value = receiver == null ? null : receiver.getContent();
        if (value == null) {
            return null;
        }
        for (var entry : value.entrySet()) {
            var key2 = entry.getKey();
            var element = entry.getValue();
            SchemaMap value2 = element instanceof SchemaMap ? (SchemaMap) element : null;
            if (value2 == null) {
                return null;
            }
            map.put(key2, value2);
        }
        return map;
    }

    public Map<String, List<SchemaMap>> mapOfListsOfMapsOrNull(String key) {
        Map<String, List<SchemaMap>> map = new LinkedHashMap<>();
        SchemaMap receiver = mapOrNull(key);
        Map<String, SchemaElement> value = receiver == null ? null : receiver.getContent();
        if (value == null) {
            return null;
        }
        for (var entry : value.entrySet()) {
            var childKey = entry.getKey();
            var element = entry.getValue();
            List<SchemaMap> list = new ArrayList<>();
            SchemaList value2 = element instanceof SchemaList ? (SchemaList) element : null;
            if (value2 == null) {
                return null;
            }
            for (var item : value2.getContent()) {
                SchemaMap value3 = item instanceof SchemaMap ? (SchemaMap) item : null;
                if (value3 == null) {
                    return null;
                }
                list.add(value3);
            }
            map.put(childKey, list);
        }
        return map;
    }

    public List<SchemaMap> listOfMaps(String key) {
        SchemaElement list = getContent().get(key);
        if (list == null) {
            throw new IllegalStateException("Missing required list at " + path(key));
        }
        SchemaList schemaList = list instanceof SchemaList ? (SchemaList) list : null;
        if (schemaList == null) {
            throw new IllegalStateException(
                    "Expected list, found invalid type " + list.getClass().getSimpleName() + " at " + path(key));
        }
        List<SchemaMap> result = new ArrayList<>();
        int index = 0;
        for (SchemaElement element : schemaList.getContent()) {
            SchemaMap value = element instanceof SchemaMap ? (SchemaMap) element : null;
            if (value == null) {
                throw new IllegalStateException("Expected map, found invalid type "
                        + ((SchemaList) list).getClass().getSimpleName() + " at " + getPath() + "[" + index + "]."
                        + key);
            }
            result.add(value);
            index++;
        }
        return result;
    }

    public List<SchemaMap> listOfMapsOrNull(String key) {
        List<SchemaMap> list = new ArrayList<>();
        SchemaList receiver = listOrNull(key);
        List<SchemaElement> value = receiver == null ? null : receiver.getContent();
        if (value == null) {
            return null;
        }
        for (var element : value) {
            SchemaMap value2 = element instanceof SchemaMap ? (SchemaMap) element : null;
            if (value2 == null) {
                return null;
            }
            list.add(value2);
        }
        return list;
    }

    private String path(String key) {
        if (getPath().isEmpty()) {
            return key;
        } else {
            return getPath() + "." + key;
        }
    }
}
