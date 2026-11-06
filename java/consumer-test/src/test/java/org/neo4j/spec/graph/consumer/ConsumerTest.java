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
package org.neo4j.spec.graph.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.GraphSpecs;
import org.neo4j.spec.graph.codec.format.JsonFormat;
import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.NameFormat;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;
import org.neo4j.spec.graph.model.node.Labels;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.property.Neo4jType;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.Validations;
import org.neo4j.spec.graph.validate.constraint.Compositeness;

/**
 * A plain Java consumer of the built library jar, covering what a customer does with it: build, encode, decode,
 * validate and migrate models, and configure the library.
 */
class ConsumerTest {
    private static final String MODEL = "{\"schema\":\"" + Version.schemaUrl(Version.LATEST)
        + "\",\"version\":\"1\",\"id\":\"movies\"}";

    @Test
    void usesTheBuiltJarWithoutKotlin() throws Exception {
        Path location = Path.of(GraphSpec.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertEquals(Path.of(System.getProperty("libraryJar")), location);
        assertThrows(ClassNotFoundException.class, () -> Class.forName("kotlin.Unit"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("kotlinx.serialization.KSerializer"));
    }

    @Test
    void defaultInstanceIsStatic() {
        assertSame(GraphSpec.Json.INSTANCE, GraphSpec.getJson());
    }

    @Test
    void buildsAndRoundTripsAModel() {
        GraphModel model = new GraphModel("1", "movies");
        Node person = new Node("Person");
        person.getProperties().put("name", new Property(Neo4jType.STRING));
        person.getConstraints().put("name_key", new NodeConstraint(ConstraintType.KEY, new ArrayList<>(List.of("name"))));
        person.getExtensions().getCustom().put("source", Map.of("system", "crm", "rows", 42L, "tags", List.of("a", "b")));
        model.getNodes().put("Person", person);

        String json = GraphSpec.getJson().encodeToString(model);
        // Decoding expands shorthand (e.g. label into labels), so compare against the expanded original
        model.longhand();
        GraphModel decoded = GraphSpec.getJson().decodeFromString(json);

        assertEquals(model, decoded);
        assertEquals("Person", decoded.getNodes().get("Person").getLabels().getIdentifier());
        assertEquals(42L, ((Map<?, ?>) decoded.getNodes().get("Person").getExtensions().getCustom().get("source")).get("rows"));
    }

    @Test
    void omitsDefaultsWhenEncoding() {
        String json = GraphSpec.getJson().encodeToString(GraphSpec.getJson().decodeFromString(MODEL));
        assertTrue(json.contains("\"id\": \"movies\""), json);
        assertFalse(json.contains("\"nodes\""), json);
        assertFalse(json.contains("\"description\""), json);
    }

    @Test
    void validatesModels() {
        GraphModel model = new GraphModel("1", "movies");
        model.getNodes().put("Person", new Node());
        List<String> codes = model.validate(Validations.getAll()).stream().map(Issue::getCode).collect(Collectors.toList());
        assertTrue(codes.contains("missing_node_identifier_label"), codes.toString());
        assertTrue(Validations.getAll().size() >= Validations.getCore().size());
    }

    @Test
    void acceptsCustomValidators() {
        Validation noRelationships = (model, issues) -> {
            if (model.getRelationships().isEmpty()) issues.add(new Issue("no_relationships", "Model has no relationships"));
        };
        List<Issue> issues = GraphModel.validate(GraphSpec.getJson().decodeFromString(MODEL), List.of(noRelationships));
        assertEquals(List.of(new Issue("no_relationships", "Model has no relationships")), issues);
    }

    @Test
    void migratesLegacyDataModels() throws Exception {
        String dataModel = Files.readString(Path.of(System.getProperty("dataModelFixture")));
        GraphModel model = GraphSpec.getJson().decodeFromString(dataModel, Type.DATA_MODEL);
        assertFalse(model.getNodes().isEmpty());
        assertTrue(model.validate(Validations.getCore()).isEmpty(), model.validate(Validations.getCore()).toString());
        String back = GraphSpec.getJson().encodeToString(model, Type.DATA_MODEL, Version.DATA_MODEL_V3_0);
        assertTrue(back.contains("\"nodeObjectTypes\""), back);
    }

    @Test
    void configuresTheJsonFormat() {
        GraphSpec compact = GraphSpecs.GraphSpec(builder -> builder.json(json -> json.disable(SerializationFeature.INDENT_OUTPUT)));
        String encoded = compact.encodeToString(compact.decodeFromString(MODEL));
        assertFalse(encoded.contains("\n"), encoded);

        GraphSpec custom = GraphSpecs.GraphSpec(builder -> builder.format(JsonFormat.getDefault()));
        assertNotNull(custom.decodeFromString(MODEL));
    }

    @Test
    void staticHelpers() {
        assertEquals("name_Person_key", NameFormat.constraintName(List.of("name"), "Person", ConstraintType.KEY));
        assertNotNull(NameFormat.indexName(List.of("name"), "Person", IndexType.TEXT));
        assertEquals(Neo4jType.STRING, Neo4jType.fromString("STRING"));
        assertTrue(Compositeness.isCompositeConstraint(List.of("a", "b")));
        assertNotNull(SchemaElements.schemaListOf("a", 1));
        assertNotNull(new Labels("Person").getImplied());
        assertEquals(Version.LATEST, Version.parseSchemaVersion(new GraphModel("1", "movies").getSchema()));
    }
}
