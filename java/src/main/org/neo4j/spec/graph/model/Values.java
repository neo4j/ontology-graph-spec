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
package org.neo4j.spec.graph.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Helpers for deep-copying free-form values into unmodifiable structures. */
final class Values {
    private Values() {}

    /**
     * Deep copies a map of plain values into an unmodifiable, insertion ordered map.
     *
     * @param source the map to copy, may be null
     * @return an unmodifiable copy, empty when the source is null
     */
    static Map<String, Object> deepUnmodifiable(Map<String, ?> source) {
        if (source == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : source.entrySet()) {
            copy.put(e.getKey(), deepUnmodifiableValue(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    private static Object deepUnmodifiableValue(Object value) {
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                copy.put(String.valueOf(e.getKey()), deepUnmodifiableValue(e.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                copy.add(deepUnmodifiableValue(item));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
