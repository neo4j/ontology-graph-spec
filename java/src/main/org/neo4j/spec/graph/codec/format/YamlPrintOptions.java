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
package org.neo4j.spec.graph.codec.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.neo4j.spec.graph.codec.schema.SchemaElement;

/** Options for {@link YamlWriter}: indent width, string quoting and which paths are written inline (flow style). */
public final class YamlPrintOptions {
    private final int indent;
    private final boolean alwaysQuoteStrings;
    private final List<String> patterns = new ArrayList<>();
    private final List<Pattern> matchers = new ArrayList<>();

    public YamlPrintOptions(int indent, boolean alwaysQuoteStrings, Set<String> inlinePaths) {
        this.indent = indent;
        this.alwaysQuoteStrings = alwaysQuoteStrings;
        for (String pattern : inlinePaths) {
            patterns.add(pattern);
            matchers.add(Pattern.compile(patternToRegex(pattern)));
        }
    }

    public YamlPrintOptions() {
        this(2, false, Set.of());
    }

    public int getIndent() {
        return indent;
    }

    public boolean isAlwaysQuoteStrings() {
        return alwaysQuoteStrings;
    }

    public boolean shouldInline(SchemaElement element) {
        if (matchers.isEmpty()) return false;
        String path = element.getPath();
        for (int i = 0; i < matchers.size(); i++) {
            if (patterns.get(i).equals(path) || matchers.get(i).matcher(path).matches()) return true;
        }
        return false;
    }

    private static String patternToRegex(String pattern) {
        StringBuilder builder = new StringBuilder("^");
        int i = 0;
        while (i < pattern.length()) {
            if (pattern.startsWith("**", i)) {
                // match any characters across multiple hierarchy levels
                builder.append(".*");
                i += 2;
            } else if (pattern.startsWith("*", i)) {
                // matches single key/index level
                builder.append("[^.]+");
                i += 1;
            } else {
                char c = pattern.charAt(i);
                if ("\\^$.|?+()[]{}".indexOf(c) >= 0) builder.append('\\');
                builder.append(c);
                i += 1;
            }
        }
        return builder.append("$").toString();
    }
}
