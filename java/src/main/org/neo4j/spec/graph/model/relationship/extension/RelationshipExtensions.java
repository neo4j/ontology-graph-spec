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
package org.neo4j.spec.graph.model.relationship.extension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.neo4j.spec.graph.model.extension.CustomExtensions;

/** Extensions for a relationship. */
public final class RelationshipExtensions implements CustomExtensions {
    private final Map<String, RelationshipIndex> indexes;

    private final Map<String, Object> custom;

    private RelationshipExtensions(Builder builder) {
        this.indexes = builder.indexes;
        this.custom = builder.custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Indexes on this relationship keyed by id. */
    public Map<String, RelationshipIndex> getIndexes() {
        return this.indexes;
    }

    /** User-defined arbitrary data. */
    @Override
    public Map<String, Object> getCustom() {
        return this.custom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RelationshipExtensions)) return false;
        RelationshipExtensions that = (RelationshipExtensions) other;
        return Objects.equals(indexes, that.indexes) && Objects.equals(custom, that.custom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes, custom);
    }

    @Override
    public String toString() {
        return "RelationshipExtensions(indexes=" + indexes + ", custom=" + custom + ")";
    }

    public static final class Builder {
        private Map<String, RelationshipIndex> indexes = new LinkedHashMap<>();
        private Map<String, Object> custom = new LinkedHashMap<>();

        private Builder() {}

        public Builder indexes(Map<String, RelationshipIndex> indexes) {
            this.indexes = indexes;
            return this;
        }

        public Builder custom(Map<String, Object> custom) {
            this.custom = custom;
            return this;
        }

        public RelationshipExtensions build() {
            return new RelationshipExtensions(this);
        }
    }
}
