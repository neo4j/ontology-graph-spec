// Generated from org/neo4j/spec/graph/codec/schema/SchemaMap.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.codec.schema;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;

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
        this(new LinkedHashMap<String, SchemaElement>(), "");
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
        return Kt.joinToString(getContent().entrySet(), ",", "{", "}", entry -> {
            var k = entry.getKey();
            var v = entry.getValue();
            StringBuilder builder = new StringBuilder();
            builder.append('"').append(k).append('"');
            builder.append(':');
            builder.append(v);
            return builder.toString();
        });
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
            throw new IllegalStateException("Expected map, found invalid type " + map.getClass().getSimpleName() + " at " + path(key));
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
            throw new IllegalStateException("Expected list, found invalid type " + list.getClass().getSimpleName() + " at " + path(key));
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
            throw new IllegalStateException("Expected map, found invalid type " + map.getClass().getSimpleName() + " at " + path(key));
        }
        return value;
    }

    public SchemaMap mapOrNull(String key) {
        SchemaElement receiver = getContent().get(key);
        return receiver == null ? null : receiver instanceof SchemaMap ? (SchemaMap) receiver : null;
    }

    public SchemaMap mapOrPut(String key) {
        return (SchemaMap) Kt.getOrPut(getContent(), key, () -> SchemaElements.adopt(this, new SchemaMap(), key));
    }

    public SchemaPrimitive literal(String key) {
        SchemaElement literal = getContent().get(key);
        if (literal == null) {
            throw new IllegalStateException("Missing required literal at " + path(key));
        }
        SchemaPrimitive value = literal instanceof SchemaPrimitive ? (SchemaPrimitive) literal : null;
        if (value == null) {
            throw new IllegalStateException("Expected literal, found invalid type " + literal.getClass().getSimpleName() + " at " + path(key));
        }
        return value;
    }

    public SchemaLiteral literalOrNull(String key) {
        SchemaElement receiver = getContent().get(key);
        return receiver == null ? null : receiver instanceof SchemaLiteral ? (SchemaLiteral) receiver : null;
    }

    public boolean bool(String key) {
        return Kt.toBooleanStrict(string(key));
    }

    public Boolean boolOrNull(String key) {
        String receiver = stringOrNull(key);
        return receiver == null ? null : Kt.toBooleanStrictOrNull(receiver);
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
        return receiver == null ? null : Kt.toIntOrNull(receiver);
    }

    public Map<String, SchemaMap> mapOfMaps(String key) {
        SchemaElement map = getContent().get(key);
        if (map == null) {
            throw new IllegalStateException("Missing required map at " + path(key));
        }
        SchemaMap schemaMap = map instanceof SchemaMap ? (SchemaMap) map : null;
        if (schemaMap == null) {
            throw new IllegalStateException("Expected map, found invalid type " + map.getClass().getSimpleName() + " at " + path(key));
        }
        return Kt.toMap(Kt.map(schemaMap.getContent(), entry -> {
            var key2 = entry.getKey();
            var element = entry.getValue();
            SchemaMap child = element instanceof SchemaMap ? (SchemaMap) element : null;
            if (child == null) {
                throw new IllegalStateException("Expected map, found invalid type " + ((SchemaMap) map).getClass().getSimpleName() + " at " + path(key2));
            }
            return new Pair<>(key2, child);
        }));
    }

    public Map<String, SchemaMap> mapOfMapsOrNull(String key) {
        Map<String, SchemaMap> map = new LinkedHashMap<String, SchemaMap>();
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
        Map<String, List<SchemaMap>> map = new LinkedHashMap<String, List<SchemaMap>>();
        SchemaMap receiver = mapOrNull(key);
        Map<String, SchemaElement> value = receiver == null ? null : receiver.getContent();
        if (value == null) {
            return null;
        }
        for (var entry : value.entrySet()) {
            var childKey = entry.getKey();
            var element = entry.getValue();
            List<SchemaMap> list = new ArrayList<SchemaMap>();
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
            throw new IllegalStateException("Expected list, found invalid type " + list.getClass().getSimpleName() + " at " + path(key));
        }
        return Kt.mapIndexed(schemaList.getContent(), (index, element) -> {
            SchemaMap value = element instanceof SchemaMap ? (SchemaMap) element : null;
            if (value == null) {
                throw new IllegalStateException("Expected map, found invalid type " + ((SchemaList) list).getClass().getSimpleName() + " at " + getPath() + "[" + index + "]." + key);
            }
            return value;
        });
    }

    public List<SchemaMap> listOfMapsOrNull(String key) {
        List<SchemaMap> list = new ArrayList<SchemaMap>();
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
