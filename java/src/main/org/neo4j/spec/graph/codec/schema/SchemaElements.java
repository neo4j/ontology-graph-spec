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

public final class SchemaElements {
    private SchemaElements() {}

    /**
     * Re-roots {@code this} at {@code newPath} in place. Descendants follow via their parent links.
     */
    public static <T extends SchemaElement> T reroot(T receiver, String newPath) {
        receiver.setParent(null);
        receiver.setIdentifier(newPath);
        return receiver;
    }

    /**
     * Adopts {@code child} so its {@code SchemaElement.path} resolves through this element.
     */
    public static <T extends SchemaElement> T adopt(SchemaElement schemaElement, T child, String identifier) {
        child.setParent(schemaElement);
        child.setIdentifier(identifier);
        return child;
    }

    public static SchemaElement toSchemaElement(Object receiver, String path) {
        if (receiver == null) {
            return new SchemaNull(path);
        } else if (receiver instanceof SchemaElement) {
            return ((SchemaElement) receiver).repath(path);
        } else if (receiver instanceof Map) {
            Map<String, SchemaElement> content = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) receiver).entrySet()) {
                content.put(String.valueOf(entry.getKey()), SchemaElements.toSchemaElement(entry.getValue()));
            }
            return new SchemaMap(content, path);
        } else if (receiver instanceof Iterable) {
            List<SchemaElement> content = new ArrayList<>();
            for (Object item : (Iterable<?>) receiver) {
                content.add(SchemaElements.toSchemaElement(item));
            }
            return new SchemaList(content, path);
        } else if (receiver instanceof String) {
            return new SchemaLiteral(((String) receiver), path, true);
        } else {
            return new SchemaLiteral(String.valueOf(receiver), path, false);
        }
    }

    public static SchemaElement toSchemaElement(Object receiver) {
        return SchemaElements.toSchemaElement(receiver, "");
    }

    public static SchemaList schemaListOf(Object... elements) {
        List<SchemaElement> content = new ArrayList<>();
        for (Object element : elements) {
            content.add(SchemaElements.toSchemaElement(element));
        }
        return new SchemaList(content);
    }

    /** Entries with a null value are skipped. */
    public static SchemaMap schemaMapOf(Map.Entry<String, ?>... entries) {
        Map<String, SchemaElement> content = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : entries) {
            if (entry.getValue() != null) {
                content.put(entry.getKey(), SchemaElements.toSchemaElement(entry.getValue()));
            }
        }
        return new SchemaMap(content);
    }

    public static <A> Map.Entry<A, Map<String, Object>> toNotEmpty(A receiver, Map<String, ?> that) {
        Map<String, Object> result = null;
        if (that != null) {
            Map<String, Object> value = ((Map<String, Object>) that);
            result = !value.isEmpty() ? value : null;
        }
        return new AbstractMap.SimpleEntry<>(receiver, result);
    }

    public static <A> Map.Entry<A, Collection<Object>> toNotEmpty(A receiver, Collection<?> that) {
        Collection<Object> result = null;
        if (that != null) {
            Collection<Object> value = ((Collection<Object>) that);
            result = !value.isEmpty() ? value : null;
        }
        return new AbstractMap.SimpleEntry<>(receiver, result);
    }
}
