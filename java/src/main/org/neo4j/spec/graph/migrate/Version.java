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
package org.neo4j.spec.graph.migrate;

import java.util.Objects;

/** A {@code major.minor.patch} version number. */
public final class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int patch;

    private Version(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version numbers can't be negative");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static Version of(int major, int minor, int patch) {
        return new Version(major, minor, patch);
    }

    /**
     * Parses a version such as {@code 1.1.0}.
     *
     * @throws IllegalArgumentException if the text isn't three dot-separated non-negative integers
     */
    public static Version parse(String text) {
        Objects.requireNonNull(text, "text");
        String[] parts = text.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Not a major.minor.patch version: " + text);
        }
        try {
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a major.minor.patch version: " + text, e);
        }
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    @Override
    public int compareTo(Version other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) {
            result = Integer.compare(minor, other.minor);
        }
        return result == 0 ? Integer.compare(patch, other.patch) : result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Version)) {
            return false;
        }
        Version other = (Version) o;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
