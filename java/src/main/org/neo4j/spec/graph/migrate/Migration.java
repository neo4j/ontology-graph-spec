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

import org.neo4j.spec.graph.codec.schema.SchemaMap;

public abstract class Migration {
    private final String fromType;

    private final String from;

    private final String toType;

    private final String to;

    protected Migration(String fromType, String from, String toType, String to) {
        this.fromType = fromType;
        this.from = from;
        this.toType = toType;
        this.to = to;
    }

    public String getFromType() {
        return this.fromType;
    }

    public String getFrom() {
        return this.from;
    }

    public String getToType() {
        return this.toType;
    }

    public String getTo() {
        return this.to;
    }

    public String getFromKey() {
        return getFromType() + ":" + getFrom();
    }

    public String getToKey() {
        return getToType() + ":" + getTo();
    }

    public abstract SchemaMap migrate(SchemaMap schema);
}
