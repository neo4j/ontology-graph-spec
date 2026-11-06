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
package org.neo4j.spec.graph;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.neo4j.spec.graph.codec.format.Format;
import org.neo4j.spec.graph.codec.format.JsonFormat;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.validate.Validation;

/**
 * Validators, migrations and format used by a {@link GraphSpec}.
 * Hand-written: unlike the Kotlin library the JSON format is configured with a Jackson {@link JsonMapper.Builder}.
 */
public final class GraphSpecConfig {
    private final List<Validation> validators;
    private final Map<String, List<Migration>> migrations;
    private final Format format;

    public GraphSpecConfig(List<Validation> validators, Map<String, List<Migration>> migrations, Format format) {
        this.validators = validators;
        this.migrations = migrations;
        this.format = format;
    }

    public List<Validation> getValidators() {
        return validators;
    }

    public Map<String, List<Migration>> getMigrations() {
        return migrations;
    }

    public Format getFormat() {
        return format;
    }

    public static final class Builder {
        private Format format;
        private final List<Validation> validators = new ArrayList<>();
        private final Map<String, List<Migration>> migrations = new LinkedHashMap<>();

        public Builder(Format format) {
            this.format = format;
        }

        public Builder(GraphSpecConfig config) {
            this(config.getFormat());
            validators.addAll(config.getValidators());
            config.getMigrations().forEach((key, value) -> migrations.put(key, new ArrayList<>(value)));
        }

        public Format getFormat() {
            return format;
        }

        public void setFormat(Format format) {
            this.format = format;
        }

        public List<Validation> getValidators() {
            return validators;
        }

        public Map<String, List<Migration>> getMigrations() {
            return migrations;
        }

        /** Configures the JSON format. Unknown keys are always ignored. */
        public void json(Consumer<JsonMapper.Builder> configure) {
            JsonMapper.Builder builder = JsonFormat.builder();
            configure.accept(builder);
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            this.format = new JsonFormat(builder.build());
        }

        public void format(Format format) {
            this.format = format;
        }

        public void validate(Validation... validation) {
            validators.addAll(Arrays.asList(validation));
        }

        public void migrate(Migration migration) {
            migrations.computeIfAbsent(migration.getFromKey(), key -> new ArrayList<>()).add(migration);
        }

        public GraphSpecConfig build() {
            return new GraphSpecConfig(validators, migrations, format);
        }
    }
}
