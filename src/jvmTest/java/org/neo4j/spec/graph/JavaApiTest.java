package org.neo4j.spec.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.neo4j.spec.graph.codec.format.JsonFormat;
import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.NameFormat;
import org.neo4j.spec.graph.model.Version;
import org.neo4j.spec.graph.model.node.Labels;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.property.Neo4jType;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.constraint.Compositeness;
import org.neo4j.spec.graph.validate.Validations;

class JavaApiTest {
    private static final String MODEL = "{\"schema\":\"" + Version.schemaUrl(Version.LATEST)
        + "\",\"version\":\"1\",\"id\":\"movies\"}";

    @Test
    void defaultInstancesAreStaticAccessors() {
        assertSame(GraphSpec.Json.INSTANCE, GraphSpec.getJson());
        assertSame(GraphSpec.Yaml.INSTANCE, GraphSpec.getYaml());
    }

    @Test
    void decodeAndEncodeUseDefaults() {
        GraphModel model = GraphSpec.getJson().decodeFromString(MODEL);
        assertEquals("movies", model.getId());
        assertTrue(GraphSpec.getJson().encodeToString(model).contains("\"movies\""));
        assertTrue(GraphSpec.getYaml().encodeToString(model).contains("movies"));
    }

    @Test
    void constructorsHaveOverloads() {
        Node node = new Node("Person");
        assertEquals("Person", node.getLabel());
        assertNotNull(new Labels("Person").getImplied());
        GraphModel model = new GraphModel("1", "movies");
        assertEquals(Version.parseSchemaVersion(model.getSchema()), Version.LATEST);
    }

    @Test
    void validationSetsAreStatic() {
        List<Issue> issues = new GraphModel("1", "movies").validate(Validations.getCore());
        assertNotNull(issues);
        assertTrue(Validations.getAll().size() >= Validations.getCore().size());
    }

    @Test
    void companionHelpersAreStatic() {
        assertEquals("name_Person_key", NameFormat.constraintName(List.of("name"), "Person",
            ConstraintType.KEY));
        assertNotNull(NameFormat.indexName(List.of("name"), "Person"));
        assertNotNull(NameFormat.indexName(List.of("name"), "Person", IndexType.TEXT));
        assertEquals(Neo4jType.STRING, Neo4jType.fromString("STRING"));
        Node node = new Node("Person");
        GraphModel model = new GraphModel("1", "movies");
        model.getNodes().put("Person", node);
        GraphModel.longhand(model);
        assertEquals("Person", node.getLabels().getIdentifier());
    }

    @Test
    void facadeClassesAreNamed() {
        assertNotNull(GraphSpecs.defaultConfig(JsonFormat.getDefault()));
        assertTrue(Compositeness.isCompositeConstraint(
            Set.of("a", "b")));
        assertNotNull(SchemaElements.schemaListOf("a", 1));
    }

    @Test
    void builderTakesJavaConsumers() {
        GraphSpec spec = GraphSpecs.graphSpec(builder -> builder.format(JsonFormat.getDefault()));
        assertNotNull(spec.decodeFromString(MODEL));

        GraphSpec custom = GraphSpecs.graphSpec(GraphSpec.getYaml(), builder ->
            GraphSpecs.json(builder, json -> json.setPrettyPrint(false)));
        assertTrue(!custom.encodeToString(spec.decodeFromString(MODEL)).contains("\n  "));
    }
}
