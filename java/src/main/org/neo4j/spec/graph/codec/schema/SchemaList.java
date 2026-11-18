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

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SchemaList extends AbstractList<SchemaElement> implements SchemaElement {
    private final List<SchemaElement> content;

    private SchemaElement parent;

    private String identifier;

    public SchemaList(List<SchemaElement> content, String path) {
        this.content = content;
        this.parent = null;
        this.identifier = path;
        int index = 0;
        for (var element : getContent()) {
            SchemaElements.adopt(this, element, "[" + index + "]");
            index++;
        }
    }

    public SchemaList(List<SchemaElement> content) {
        this(content, "");
    }

    @Override
    public SchemaElement get(int index) {
        return this.content.get(index);
    }

    @Override
    public int size() {
        return this.content.size();
    }

    @Override
    public SchemaElement set(int index, SchemaElement element) {
        return this.content.set(index, element);
    }

    @Override
    public void add(int index, SchemaElement element) {
        this.content.add(index, element);
    }

    @Override
    public SchemaElement remove(int index) {
        return this.content.remove(index);
    }

    public List<SchemaElement> getContent() {
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
        return getContent().stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }
}
