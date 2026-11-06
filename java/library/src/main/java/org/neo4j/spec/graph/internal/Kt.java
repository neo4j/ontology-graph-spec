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
package org.neo4j.spec.graph.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The subset of the Kotlin standard library used by the generated sources, with the same semantics: insertion ordered
 * maps and sets, lists for collection results, Kotlin's definition of whitespace.
 *
 * <p>Internal to the library, not part of its API.
 */
public final class Kt {
    private Kt() {
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Errors
    // -----------------------------------------------------------------------------------------------------------------

    /** {@code error(message)} in expression position. */
    public static <T> T error(Object message) {
        throw new IllegalStateException(String.valueOf(message));
    }

    /** Kotlin's covariant read-only collections: an upcast Java generics can't express. */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object value) {
        return (T) value;
    }

    /** Placeholder the generator emits for constructs it couldn't translate. */
    public static <T> T todo(String construct) {
        throw new UnsupportedOperationException("Not translated from Kotlin: " + construct);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Collection factories
    // -----------------------------------------------------------------------------------------------------------------

    public static <T> List<T> emptyList() {
        return Collections.emptyList();
    }

    public static <K, V> Map<K, V> emptyMap() {
        return Collections.emptyMap();
    }

    public static <T> Set<T> emptySet() {
        return Collections.emptySet();
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        if (elements.length == 0) return Collections.emptyList();
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(elements)));
    }

    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Pair<? extends K, ? extends V>... pairs) {
        return Collections.unmodifiableMap(mutableMapOf(pairs));
    }

    @SafeVarargs
    public static <T> List<T> mutableListOf(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    @SafeVarargs
    public static <T> Set<T> mutableSetOf(T... elements) {
        return new LinkedHashSet<>(Arrays.asList(elements));
    }

    @SafeVarargs
    public static <K, V> Map<K, V> mutableMapOf(Pair<? extends K, ? extends V>... pairs) {
        Map<K, V> map = new LinkedHashMap<>();
        for (Pair<? extends K, ? extends V> pair : pairs) map.put(pair.getFirst(), pair.getSecond());
        return map;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Null handling
    // -----------------------------------------------------------------------------------------------------------------

    public static <T> List<T> orEmpty(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public static <T> Set<T> orEmpty(Set<T> set) {
        return set == null ? Collections.emptySet() : set;
    }

    public static <T> Collection<T> orEmpty(Collection<T> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }

    public static <K, V> Map<K, V> orEmpty(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    public static String orEmpty(String string) {
        return string == null ? "" : string;
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNullOrEmpty(CharSequence string) {
        return string == null || string.length() == 0;
    }

    public static boolean isNullOrBlank(CharSequence string) {
        return string == null || isBlank(string);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Iterables
    // -----------------------------------------------------------------------------------------------------------------

    public static <T, R> List<R> map(Iterable<T> iterable, Function<? super T, ? extends R> transform) {
        List<R> result = new ArrayList<>();
        for (T item : iterable) result.add(transform.apply(item));
        return result;
    }

    public static <K, V, R> List<R> map(Map<K, V> map, Function<? super Map.Entry<K, V>, ? extends R> transform) {
        return map(map.entrySet(), transform);
    }

    public static <T, R, C extends Collection<? super R>> C mapTo(
            Iterable<T> iterable, C destination, Function<? super T, ? extends R> transform) {
        for (T item : iterable) destination.add(transform.apply(item));
        return destination;
    }

    public static <T, R> List<R> mapIndexed(Iterable<T> iterable, BiFunction<Integer, ? super T, ? extends R> transform) {
        List<R> result = new ArrayList<>();
        int index = 0;
        for (T item : iterable) result.add(transform.apply(index++, item));
        return result;
    }

    public static <T, R> List<R> mapNotNull(Iterable<T> iterable, Function<? super T, ? extends R> transform) {
        List<R> result = new ArrayList<>();
        for (T item : iterable) {
            R value = transform.apply(item);
            if (value != null) result.add(value);
        }
        return result;
    }

    public static <K, V, R> List<R> mapNotNull(Map<K, V> map, Function<? super Map.Entry<K, V>, ? extends R> transform) {
        return mapNotNull(map.entrySet(), transform);
    }

    public static <T, R> List<R> flatMap(Iterable<T> iterable, Function<? super T, ? extends Iterable<? extends R>> transform) {
        List<R> result = new ArrayList<>();
        for (T item : iterable) for (R value : transform.apply(item)) result.add(value);
        return result;
    }

    public static <T> List<T> flatten(Iterable<? extends Iterable<? extends T>> iterable) {
        List<T> result = new ArrayList<>();
        for (Iterable<? extends T> inner : iterable) for (T value : inner) result.add(value);
        return result;
    }

    public static <T> List<T> filter(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        List<T> result = new ArrayList<>();
        for (T item : iterable) if (predicate.apply(item)) result.add(item);
        return result;
    }

    public static <T> List<T> filter(T[] array, Function<? super T, Boolean> predicate) {
        return filter(Arrays.asList(array), predicate);
    }

    public static <T, R> List<R> map(T[] array, Function<? super T, ? extends R> transform) {
        return map(Arrays.asList(array), transform);
    }

    public static <T> List<T> toList(T[] array) {
        return new ArrayList<>(Arrays.asList(array));
    }

    public static <T> List<T> filterNot(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        List<T> result = new ArrayList<>();
        for (T item : iterable) if (!predicate.apply(item)) result.add(item);
        return result;
    }

    public static <R> List<R> filterIsInstance(Iterable<?> iterable, Class<R> type) {
        List<R> result = new ArrayList<>();
        for (Object item : iterable) if (type.isInstance(item)) result.add(type.cast(item));
        return result;
    }

    public static <T> boolean any(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        for (T item : iterable) if (predicate.apply(item)) return true;
        return false;
    }

    public static boolean any(Iterable<?> iterable) {
        return iterable.iterator().hasNext();
    }

    public static <K, V> boolean any(Map<K, V> map, Function<? super Map.Entry<K, V>, Boolean> predicate) {
        return any(map.entrySet(), predicate);
    }

    public static boolean any(CharSequence string, Function<Character, Boolean> predicate) {
        for (int i = 0; i < string.length(); i++) if (predicate.apply(string.charAt(i))) return true;
        return false;
    }

    public static <T> int count(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        int count = 0;
        for (T item : iterable) if (predicate.apply(item)) count++;
        return count;
    }

    public static int count(CharSequence string, Function<Character, Boolean> predicate) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) if (predicate.apply(string.charAt(i))) count++;
        return count;
    }

    public static <T> boolean contains(Iterable<T> iterable, Object element) {
        if (iterable instanceof Collection) return ((Collection<?>) iterable).contains(element);
        for (T item : iterable) if (Objects.equals(item, element)) return true;
        return false;
    }

    public static <T> T first(Iterable<T> iterable) {
        Iterator<T> iterator = iterable.iterator();
        if (!iterator.hasNext()) throw new NoSuchElementException("Collection is empty.");
        return iterator.next();
    }

    public static <T> T first(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        for (T item : iterable) if (predicate.apply(item)) return item;
        throw new NoSuchElementException("Collection contains no element matching the predicate.");
    }

    public static <T> T firstOrNull(Iterable<T> iterable) {
        Iterator<T> iterator = iterable.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static <T> T firstOrNull(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        for (T item : iterable) if (predicate.apply(item)) return item;
        return null;
    }

    public static <T> T find(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        return firstOrNull(iterable, predicate);
    }

    public static <T, R> R firstNotNullOfOrNull(Iterable<T> iterable, Function<? super T, ? extends R> transform) {
        for (T item : iterable) {
            R value = transform.apply(item);
            if (value != null) return value;
        }
        return null;
    }

    public static <T> T last(List<T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("List is empty.");
        return list.get(list.size() - 1);
    }

    public static <T> T singleOrNull(Iterable<T> iterable) {
        Iterator<T> iterator = iterable.iterator();
        if (!iterator.hasNext()) return null;
        T single = iterator.next();
        return iterator.hasNext() ? null : single;
    }

    public static <T> T singleOrNull(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        T single = null;
        boolean found = false;
        for (T item : iterable) {
            if (predicate.apply(item)) {
                if (found) return null;
                single = item;
                found = true;
            }
        }
        return single;
    }

    public static <T> T getOrNull(List<T> list, int index) {
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    public static <T extends Comparable<? super T>> T min(Iterable<T> iterable) {
        Iterator<T> iterator = iterable.iterator();
        if (!iterator.hasNext()) throw new NoSuchElementException();
        T min = iterator.next();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (next.compareTo(min) < 0) min = next;
        }
        return min;
    }

    public static <T> T removeFirst(List<T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("List is empty.");
        return list.remove(0);
    }

    public static <T> List<T> drop(Iterable<T> iterable, int n) {
        List<T> result = new ArrayList<>();
        int index = 0;
        for (T item : iterable) if (index++ >= n) result.add(item);
        return result;
    }

    public static <T> List<T> take(Iterable<T> iterable, int n) {
        List<T> result = new ArrayList<>();
        for (T item : iterable) {
            if (result.size() >= n) break;
            result.add(item);
        }
        return result;
    }

    public static <T> List<T> distinct(T[] array) {
        return distinct(Arrays.asList(array));
    }

    public static <K, V> Map<K, V> filter(Map<K, V> map, Function<? super Map.Entry<K, V>, Boolean> predicate) {
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) if (predicate.apply(entry)) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    public static <T> List<T> distinct(Iterable<T> iterable) {
        return new ArrayList<>(toSet(iterable));
    }

    public static <T extends Comparable<? super T>> List<T> sorted(Iterable<T> iterable) {
        List<T> result = toMutableList(iterable);
        Collections.sort(result);
        return result;
    }

    public static List<Integer> indices(Collection<?> collection) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < collection.size(); i++) result.add(i);
        return result;
    }

    public static <T> List<IndexedValue<T>> withIndex(Iterable<T> iterable) {
        List<IndexedValue<T>> result = new ArrayList<>();
        int index = 0;
        for (T item : iterable) result.add(new IndexedValue<>(index++, item));
        return result;
    }

    public static <T> Pair<List<T>, List<T>> partition(Iterable<T> iterable, Function<? super T, Boolean> predicate) {
        List<T> first = new ArrayList<>();
        List<T> second = new ArrayList<>();
        for (T item : iterable) (predicate.apply(item) ? first : second).add(item);
        return new Pair<>(first, second);
    }

    public static <T> List<T> toList(Iterable<T> iterable) {
        return toMutableList(iterable);
    }

    public static <T> List<T> toMutableList(Iterable<T> iterable) {
        List<T> result = new ArrayList<>();
        for (T item : iterable) result.add(item);
        return result;
    }

    public static <T> Set<T> toSet(Iterable<T> iterable) {
        Set<T> result = new LinkedHashSet<>();
        for (T item : iterable) result.add(item);
        return result;
    }

    public static <T> String joinToString(Iterable<T> iterable) {
        return joinToString(iterable, ", ", "", "", null);
    }

    public static <T> String joinToString(Iterable<T> iterable, CharSequence separator) {
        return joinToString(iterable, separator, "", "", null);
    }

    public static <T> String joinToString(Iterable<T> iterable, Function<? super T, ? extends CharSequence> transform) {
        return joinToString(iterable, ", ", "", "", transform);
    }

    public static <T> String joinToString(
            Iterable<T> iterable, CharSequence separator, Function<? super T, ? extends CharSequence> transform) {
        return joinToString(iterable, separator, "", "", transform);
    }

    public static <T> String joinToString(Iterable<T> iterable, CharSequence separator, CharSequence prefix, CharSequence postfix) {
        return joinToString(iterable, separator, prefix, postfix, null);
    }

    public static <T> String joinToString(
            Iterable<T> iterable,
            CharSequence separator,
            CharSequence prefix,
            CharSequence postfix,
            Function<? super T, ? extends CharSequence> transform) {
        StringBuilder builder = new StringBuilder(prefix);
        boolean first = true;
        for (T item : iterable) {
            if (!first) builder.append(separator);
            first = false;
            builder.append(transform == null ? String.valueOf(item) : transform.apply(item));
        }
        return builder.append(postfix).toString();
    }

    public static <T> List<T> plus(Collection<? extends T> collection, T element) {
        List<T> result = new ArrayList<>(collection);
        result.add(element);
        return result;
    }

    public static <T> Set<T> plusAll(Set<? extends T> set, Iterable<? extends T> elements) {
        Set<T> result = new LinkedHashSet<>(set);
        for (T element : elements) result.add(element);
        return result;
    }

    public static <T> List<T> plusAll(Collection<? extends T> collection, Iterable<? extends T> elements) {
        List<T> result = new ArrayList<>(collection);
        for (T element : elements) result.add(element);
        return result;
    }

    public static <T> List<T> minusAll(Iterable<? extends T> iterable, Iterable<?> elements) {
        Set<Object> remove = new LinkedHashSet<>();
        for (Object element : elements) remove.add(element);
        List<T> result = new ArrayList<>();
        for (T item : iterable) if (!remove.contains(item)) result.add(item);
        return result;
    }

    public static <T> boolean addAll(Collection<? super T> collection, Iterable<? extends T> elements) {
        boolean changed = false;
        for (T element : elements) changed |= collection.add(element);
        return changed;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Maps
    // -----------------------------------------------------------------------------------------------------------------

    public static <T, K, V> Map<K, V> associate(Iterable<T> iterable, Function<? super T, ? extends Pair<? extends K, ? extends V>> transform) {
        return associateTo(iterable, new LinkedHashMap<>(), transform);
    }

    public static <T, K, V, M extends Map<? super K, ? super V>> M associateTo(
            Iterable<T> iterable, M destination, Function<? super T, ? extends Pair<? extends K, ? extends V>> transform) {
        for (T item : iterable) {
            Pair<? extends K, ? extends V> pair = transform.apply(item);
            destination.put(pair.getFirst(), pair.getSecond());
        }
        return destination;
    }

    public static <T, K> Map<K, T> associateBy(Iterable<T> iterable, Function<? super T, ? extends K> key) {
        Map<K, T> result = new LinkedHashMap<>();
        for (T item : iterable) result.put(key.apply(item), item);
        return result;
    }

    public static <T, K, V> Map<K, V> associateBy(
            Iterable<T> iterable, Function<? super T, ? extends K> key, Function<? super T, ? extends V> value) {
        Map<K, V> result = new LinkedHashMap<>();
        for (T item : iterable) result.put(key.apply(item), value.apply(item));
        return result;
    }

    public static <T, K> Map<K, List<T>> groupBy(Iterable<T> iterable, Function<? super T, ? extends K> key) {
        Map<K, List<T>> result = new LinkedHashMap<>();
        for (T item : iterable) result.computeIfAbsent(key.apply(item), k -> new ArrayList<>()).add(item);
        return result;
    }

    public static <K, V, R> Map<K, R> mapValues(Map<K, V> map, Function<? super Map.Entry<K, V>, ? extends R> transform) {
        Map<K, R> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) result.put(entry.getKey(), transform.apply(entry));
        return result;
    }

    public static <K, V> Map<K, V> filterValues(Map<K, V> map, Function<? super V, Boolean> predicate) {
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) if (predicate.apply(entry.getValue())) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    public static <K, V> Map<K, V> toMap(Iterable<? extends Pair<? extends K, ? extends V>> pairs) {
        Map<K, V> result = new LinkedHashMap<>();
        for (Pair<? extends K, ? extends V> pair : pairs) result.put(pair.getFirst(), pair.getSecond());
        return result;
    }

    public static <K, V> Map<K, V> toMap(Map<? extends K, ? extends V> map) {
        return new LinkedHashMap<>(map);
    }

    public static <K, V> Map<K, V> plusEntry(Map<? extends K, ? extends V> map, Pair<? extends K, ? extends V> pair) {
        Map<K, V> result = new LinkedHashMap<>(map);
        result.put(pair.getFirst(), pair.getSecond());
        return result;
    }

    public static <K, V> Map<K, V> plusMap(Map<? extends K, ? extends V> map, Map<? extends K, ? extends V> other) {
        Map<K, V> result = new LinkedHashMap<>(map);
        result.putAll(other);
        return result;
    }

    public static <K, V> V getOrPut(Map<K, V> map, K key, Supplier<? extends V> defaultValue) {
        V value = map.get(key);
        if (value == null) {
            value = defaultValue.get();
            map.put(key, value);
        }
        return value;
    }

    public static <K, V> V getValue(Map<K, V> map, K key) {
        V value = map.get(key);
        if (value == null && !map.containsKey(key)) {
            throw new NoSuchElementException("Key " + key + " is missing in the map.");
        }
        return value;
    }

    public static boolean containsKey(Map<?, ?> map, Object key) {
        return map.containsKey(key);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Text
    // -----------------------------------------------------------------------------------------------------------------

    /** Kotlin's {@code Char.isWhitespace()}. */
    public static boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    public static boolean isBlank(CharSequence string) {
        for (int i = 0; i < string.length(); i++) if (!isWhitespace(string.charAt(i))) return false;
        return true;
    }

    public static boolean isNotBlank(CharSequence string) {
        return !isBlank(string);
    }

    public static String trim(String string) {
        int start = 0;
        int end = string.length();
        while (start < end && isWhitespace(string.charAt(start))) start++;
        while (end > start && isWhitespace(string.charAt(end - 1))) end--;
        return string.substring(start, end);
    }

    public static String trimEnd(String string) {
        int end = string.length();
        while (end > 0 && isWhitespace(string.charAt(end - 1))) end--;
        return string.substring(0, end);
    }

    public static String trimEnd(String string, char... chars) {
        int end = string.length();
        while (end > 0 && contains(chars, string.charAt(end - 1))) end--;
        return string.substring(0, end);
    }

    private static boolean contains(char[] chars, char c) {
        for (char candidate : chars) if (candidate == c) return true;
        return false;
    }

    public static boolean contains(CharSequence string, char c) {
        return string.toString().indexOf(c) >= 0;
    }

    public static boolean contains(CharSequence string, CharSequence other, boolean ignoreCase) {
        if (!ignoreCase) return string.toString().contains(other);
        return string.toString().toLowerCase(java.util.Locale.ROOT).contains(other.toString().toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean startsWith(String string, char c) {
        return !string.isEmpty() && string.charAt(0) == c;
    }

    public static boolean endsWith(String string, char c) {
        return !string.isEmpty() && string.charAt(string.length() - 1) == c;
    }

    public static boolean startsWith(String string, String prefix, boolean ignoreCase) {
        return string.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
    }

    public static String removePrefix(String string, CharSequence prefix) {
        return string.startsWith(prefix.toString()) ? string.substring(prefix.length()) : string;
    }

    public static String removeSuffix(String string, CharSequence suffix) {
        return string.endsWith(suffix.toString()) ? string.substring(0, string.length() - suffix.length()) : string;
    }

    public static String substringBefore(String string, String delimiter) {
        int index = string.indexOf(delimiter);
        return index == -1 ? string : string.substring(0, index);
    }

    public static String substringBefore(String string, char delimiter) {
        int index = string.indexOf(delimiter);
        return index == -1 ? string : string.substring(0, index);
    }

    public static String substringBeforeLast(String string, char delimiter) {
        int index = string.lastIndexOf(delimiter);
        return index == -1 ? string : string.substring(0, index);
    }

    public static String substringBeforeLast(String string, String delimiter) {
        int index = string.lastIndexOf(delimiter);
        return index == -1 ? string : string.substring(0, index);
    }

    public static String substringAfterLast(String string, char delimiter) {
        int index = string.lastIndexOf(delimiter);
        return index == -1 ? string : string.substring(index + 1);
    }

    public static String substringAfterLast(String string, String delimiter) {
        int index = string.lastIndexOf(delimiter);
        return index == -1 ? string : string.substring(index + delimiter.length());
    }

    public static String take(String string, int n) {
        if (n < 0) throw new IllegalArgumentException("Requested character count " + n + " is less than zero.");
        return string.substring(0, Math.min(n, string.length()));
    }

    public static String drop(String string, int n) {
        if (n < 0) throw new IllegalArgumentException("Requested character count " + n + " is less than zero.");
        return string.substring(Math.min(n, string.length()));
    }

    public static String takeWhile(String string, Function<Character, Boolean> predicate) {
        for (int i = 0; i < string.length(); i++) if (!predicate.apply(string.charAt(i))) return string.substring(0, i);
        return string;
    }

    public static Integer toIntOrNull(String string) {
        if (string.isEmpty()) return null;
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long toLongOrNull(String string) {
        if (string.isEmpty()) return null;
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double toDoubleOrNull(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean toBooleanStrict(String string) {
        Boolean value = toBooleanStrictOrNull(string);
        if (value == null) throw new IllegalArgumentException("The string doesn't represent a boolean value: " + string);
        return value;
    }

    public static Boolean toBooleanStrictOrNull(String string) {
        if ("true".equals(string)) return true;
        if ("false".equals(string)) return false;
        return null;
    }
}
