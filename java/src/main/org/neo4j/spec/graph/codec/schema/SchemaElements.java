// Generated from org/neo4j/spec/graph/codec/schema/SchemaElement.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.codec.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;

public final class SchemaElements {
    private SchemaElements() {
    }

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
    public static <T extends SchemaElement> T adopt(SchemaElement schemaElement, T child,
            String identifier) {
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
            return new SchemaMap(Kt.associateTo(((Map<?, ?>) receiver).entrySet(), new LinkedHashMap<String, SchemaElement>(), entry -> {
                var k = entry.getKey();
                var v = entry.getValue();
                return new Pair<>(String.valueOf(k), SchemaElements.toSchemaElement(v));
            }), path);
        } else if (receiver instanceof Iterable) {
            return new SchemaList(Kt.mapTo(((Iterable<?>) receiver), new ArrayList<SchemaElement>(), it -> SchemaElements.toSchemaElement(it)), path);
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
        return new SchemaList(Kt.toMutableList(Kt.map(elements, it -> SchemaElements.toSchemaElement(it))));
    }

    public static SchemaMap schemaMapOf(Pair<String, ?>... pairs) {
        return new SchemaMap(Kt.associateTo(Kt.filter(pairs, it -> it.getSecond() != null), new LinkedHashMap<String, SchemaElement>(), it -> new Pair<>(it.getFirst(), SchemaElements.toSchemaElement(Objects.requireNonNull(it.getSecond())))));
    }

    public static <A> Pair<A, Map<String, Object>> toNotEmpty(A receiver, Map<String, ?> that) {
        Map<String, Object> result = null;
        if (that != null) {
            Map<String, Object> value = ((Map<String, Object>) that);
            result = !value.isEmpty() ? value : null;
        }
        return new Pair<>(receiver, result);
    }

    public static <A> Pair<A, Collection<Object>> toNotEmpty(A receiver, Collection<?> that) {
        Collection<Object> result = null;
        if (that != null) {
            Collection<Object> value = ((Collection<Object>) that);
            result = !value.isEmpty() ? value : null;
        }
        return new Pair<>(receiver, result);
    }
}
