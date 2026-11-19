package org.neo4j.spec.graph.spectests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.model.ConstraintType;
import org.neo4j.spec.graph.model.FulltextIndexOptions;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.GraphModelExtensions;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.model.Labels;
import org.neo4j.spec.graph.model.Mapping;
import org.neo4j.spec.graph.model.Neo4jType;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.NodeExtensions;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.NodeMapping;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.model.RelationshipExtensions;
import org.neo4j.spec.graph.model.RelationshipIndex;
import org.neo4j.spec.graph.model.RelationshipMapping;
import org.neo4j.spec.graph.model.RelationshipTarget;
import org.neo4j.spec.graph.model.Table;
import org.neo4j.spec.graph.model.TableColumn;
import org.neo4j.spec.graph.model.TargetMapping;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;
import org.neo4j.spec.graph.validate.ValidatorSet;
import org.neo4j.spec.graph.validate.Validators;
import org.neo4j.spec.graph.validate.consistent.ConsistentSchemaValidators;
import org.neo4j.spec.graph.validate.core.CoreValidators;
import org.neo4j.spec.graph.validate.labelled.LabelledValidators;
import org.neo4j.spec.graph.validate.wellconstrained.WellConstrainedValidators;

/** Unit tests of the validation engine and the core validators, using models built in code. */
class ValidationTests {

    private static final String SCHEMA = "https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json";

    private static GraphModel.Builder model() {
        return GraphModel.builder().schema(SCHEMA).version("1").id("test");
    }

    private static RelationshipTarget end() {
        return RelationshipTarget.builder().build();
    }

    private static Relationship.Builder knows() {
        return Relationship.builder().type("KNOWS").from(end()).to(end());
    }

    private static Property.Builder property(Neo4jType type) {
        return Property.builder().type(type);
    }

    private static List<Issue> core(GraphModel model) {
        return Validators.core().validate(model);
    }

    private static void assertIssue(List<Issue> issues, String code, String path, Map<String, String> details) {
        List<Issue> matches = issues.stream()
                .filter(i -> i.code().equals(code) && i.path().equals(java.util.Optional.of(path)))
                .collect(Collectors.toList());
        assertEquals(1, matches.size(), "issue " + code + " at " + path + " in " + issues);
        assertEquals(details, matches.get(0).details());
        assertFalse(matches.get(0).message().isBlank());
    }

    private static String repeat(char c, int times) {
        return String.valueOf(c).repeat(times);
    }

    // ---- core validators ----

    @Test
    void validModelHasNoIssues() {
        GraphModel model = model().node(
                        "person",
                        Node.builder()
                                .property("name", property(Neo4jType.STRING).build())
                                .constraint(
                                        "name_key",
                                        NodeConstraint.builder()
                                                .type(ConstraintType.KEY)
                                                .property("name")
                                                .build())
                                .build())
                .relationship("knows", knows().build())
                .build();
        assertEquals(List.of(), core(model));
    }

    @Test
    void schemaUrl() {
        assertEquals(List.of(), CoreValidators.SCHEMA_URL.validate(model().build()));
        GraphModel unknown = model().schema("https://example.com/schema.json").build();
        assertIssue(CoreValidators.SCHEMA_URL.validate(unknown), "unknown_schema", "schema", Map.of());
        GraphModel future = model()
                .schema("https://neo4j.com/docs/ontology-graph-spec/9.0.0/schema.json")
                .build();
        assertEquals(1, core(future).size());
    }

    @Test
    void references() {
        Node person = Node.builder()
                .property("name", property(Neo4jType.STRING).build())
                .constraint(
                        "email_unique",
                        NodeConstraint.builder()
                                .type(ConstraintType.UNIQUE)
                                .property("name")
                                .property("email")
                                .build())
                .extensions(NodeExtensions.builder()
                        .index(
                                "email_range",
                                NodeIndex.builder()
                                        .type(IndexType.RANGE)
                                        .property("email")
                                        .build())
                        .build())
                .build();
        Relationship rel = Relationship.builder()
                .type("KNOWS")
                .from(RelationshipTarget.builder().node("person").build())
                .to(RelationshipTarget.builder().node("robot").build())
                .constraint(
                        "w",
                        RelationshipConstraint.builder()
                                .type(ConstraintType.UNIQUE)
                                .property("weight")
                                .build())
                .extensions(RelationshipExtensions.builder()
                        .index(
                                "w_idx",
                                RelationshipIndex.builder()
                                        .type(IndexType.RANGE)
                                        .property("weight")
                                        .build())
                        .build())
                .build();
        Relationship built = Relationship.builder()
                .type("BUILT")
                .from(RelationshipTarget.builder().node("robot").build())
                .to(RelationshipTarget.builder().node("person").build())
                .build();
        List<Issue> issues = CoreValidators.REFERENCES.validate(model().node("person", person)
                .relationship("knows", rel)
                .relationship("built", built)
                .build());
        assertEquals(6, issues.size(), issues.toString());
        assertIssue(issues, "missing_node_constraint_property", "nodes.person.constraints.email_unique.properties.email", Map.of());
        assertIssue(issues, "missing_node_index_property", "nodes.person.extensions.indexes.email_range.properties.email", Map.of());
        assertIssue(issues, "missing_relationship_constraint_property", "relationships.knows.constraints.w.properties.weight", Map.of());
        assertIssue(issues, "missing_relationship_index_property", "relationships.knows.extensions.indexes.w_idx.properties.weight", Map.of());
        assertIssue(issues, "missing_relationship_to_node", "relationships.knows.to.robot", Map.of());
        assertIssue(issues, "missing_relationship_from_node", "relationships.built.from.robot", Map.of());
    }

    @Test
    void relationshipMappingKeys() {
        Relationship rel = knows().property("since", property(Neo4jType.DATE).build()).build();
        Mapping valid = RelationshipMapping.builder()
                .relationship("knows")
                .table("t")
                .fromNode(TargetMapping.builder().build())
                .toNode(TargetMapping.builder().build())
                .keyProperty("since")
                .build();
        Mapping invalid = RelationshipMapping.builder()
                .relationship("knows")
                .table("t")
                .fromNode(TargetMapping.builder().build())
                .toNode(TargetMapping.builder().build())
                .keyProperty("weight")
                .build();
        Mapping unknownRelationship = RelationshipMapping.builder()
                .relationship("nope")
                .table("t")
                .fromNode(TargetMapping.builder().build())
                .toNode(TargetMapping.builder().build())
                .keyProperty("weight")
                .build();
        GraphModel model = model().relationship("knows", rel)
                .extensions(GraphModelExtensions.builder()
                        .dataSourceMapping("load", List.of(valid, invalid, unknownRelationship))
                        .build())
                .build();
        List<Issue> issues = CoreValidators.REFERENCES.validate(model);
        assertEquals(1, issues.size(), issues.toString());
        assertIssue(
                issues,
                "invalid_relationship_mapping_key",
                "extensions.data_source_mappings.load[1].key",
                Map.of("relationship", "knows", "property", "weight"));
    }

    @Test
    void constraintPropertyCounts() {
        Node person = Node.builder()
                .property("a", property(Neo4jType.STRING).build())
                .property("b", property(Neo4jType.STRING).build())
                .constraint("ok", NodeConstraint.builder().type(ConstraintType.EXISTS).property("a").build())
                .constraint("none", NodeConstraint.builder().type(ConstraintType.EXISTS).build())
                .constraint(
                        "two",
                        NodeConstraint.builder()
                                .type(ConstraintType.PROPERTY_TYPE)
                                .property("a")
                                .property("b")
                                .build())
                .constraint(
                        "key",
                        NodeConstraint.builder()
                                .type(ConstraintType.KEY)
                                .property("a")
                                .property("b")
                                .build())
                .build();
        Relationship rel = knows().property("a", property(Neo4jType.STRING).build())
                .constraint("none", RelationshipConstraint.builder().type(ConstraintType.EXISTS).build())
                .constraint(
                        "two",
                        RelationshipConstraint.builder()
                                .type(ConstraintType.PROPERTY_TYPE)
                                .property("a")
                                .property("a")
                                .build())
                .build();
        List<Issue> issues = CoreValidators.CONSTRAINT_PROPERTY_COUNTS.validate(
                model().node("person", person).relationship("knows", rel).build());
        assertEquals(4, issues.size(), issues.toString());
        assertIssue(issues, "invalid_node_exist_constraint_property_count", "nodes.person.constraints.none.properties", Map.of());
        assertIssue(issues, "invalid_node_type_constraint_property_count", "nodes.person.constraints.two.properties", Map.of());
        assertIssue(issues, "invalid_relationship_exist_constraint_property_count", "relationships.knows.constraints.none.properties", Map.of());
        assertIssue(issues, "invalid_relationship_type_constraint_property_count", "relationships.knows.constraints.two.properties", Map.of());
    }

    @Test
    void indexOptions() {
        Node person = Node.builder()
                .extensions(NodeExtensions.builder()
                        .index(
                                "bad",
                                NodeIndex.builder()
                                        .type(IndexType.TEXT)
                                        .options(FulltextIndexOptions.builder().build())
                                        .build())
                        .index(
                                "good",
                                NodeIndex.builder()
                                        .type(IndexType.FULLTEXT)
                                        .options(FulltextIndexOptions.builder().build())
                                        .build())
                        .index("none", NodeIndex.builder().type(IndexType.TEXT).build())
                        .build())
                .build();
        Relationship rel = knows().extensions(RelationshipExtensions.builder()
                        .index(
                                "bad",
                                RelationshipIndex.builder()
                                        .type(IndexType.RANGE)
                                        .options(FulltextIndexOptions.builder().build())
                                        .build())
                        .build())
                .build();
        List<Issue> issues = CoreValidators.INDEX_OPTIONS.validate(
                model().node("person", person).relationship("knows", rel).build());
        assertEquals(2, issues.size(), issues.toString());
        assertIssue(issues, "node_index_type_options_mismatch", "nodes.person.extensions.indexes.bad.options", Map.of());
        assertIssue(issues, "relationship_index_type_options_mismatch", "relationships.knows.extensions.indexes.bad.options", Map.of());
    }

    @Test
    void propertyPatterns() {
        Property both = property(Neo4jType.STRING).pattern("^[A-Z]$").oneOfValue("A").build();
        Property pattern = property(Neo4jType.STRING).pattern("^[A-Z]$").build();
        Property oneOf = property(Neo4jType.STRING).oneOfValue("A").build();
        List<Issue> issues = CoreValidators.PROPERTY_PATTERNS.validate(model().node(
                        "person", Node.builder().property("both", both).property("p", pattern).property("o", oneOf).build())
                .relationship("knows", knows().property("both", both).build())
                .build());
        assertEquals(2, issues.size(), issues.toString());
        assertIssue(issues, "property_pattern_one_of_conflict", "nodes.person.properties.both", Map.of());
        assertIssue(issues, "property_pattern_one_of_conflict", "relationships.knows.properties.both", Map.of());
    }

    @Test
    void relationshipCounts() {
        Relationship rel = Relationship.builder()
                .type("A")
                .from(RelationshipTarget.builder().count(1).maxCount(2).build())
                .to(RelationshipTarget.builder().count(-1).build())
                .build();
        Relationship other = Relationship.builder()
                .type("B")
                .from(RelationshipTarget.builder().minCount(-1).build())
                .to(RelationshipTarget.builder().minCount(3).maxCount(2).build())
                .build();
        Relationship fine = Relationship.builder()
                .type("C")
                .from(RelationshipTarget.builder().minCount(2).maxCount(2).build())
                .to(RelationshipTarget.builder().count(0).build())
                .build();
        List<Issue> issues = CoreValidators.RELATIONSHIP_COUNTS.validate(model().relationship("a", rel)
                .relationship("b", other)
                .relationship("c", fine)
                .build());
        assertEquals(4, issues.size(), issues.toString());
        assertIssue(issues, "relationship_count_conflict", "relationships.a.from", Map.of());
        assertIssue(issues, "relationship_count_negative", "relationships.a.to.count", Map.of());
        assertIssue(issues, "relationship_min_count_negative", "relationships.b.from.min_count", Map.of());
        assertIssue(issues, "relationship_min_count_exceeds_max", "relationships.b.to", Map.of());
    }

    @Test
    void repeatedEntries() {
        Node person = Node.builder()
                .labels(Labels.builder()
                        .identifier("Person")
                        .impliedLabel("Agent")
                        .impliedLabel("Agent")
                        .optionalLabel("Actor")
                        .optionalLabel("Actor")
                        .build())
                .alias("Human")
                .alias("Human")
                .property("name", property(Neo4jType.STRING).alias("x").alias("y").alias("x").build())
                .constraint(
                        "u",
                        NodeConstraint.builder()
                                .type(ConstraintType.UNIQUE)
                                .property("name")
                                .property("name")
                                .build())
                .extensions(NodeExtensions.builder()
                        .index(
                                "i",
                                NodeIndex.builder()
                                        .type(IndexType.RANGE)
                                        .label("Person")
                                        .label("Person")
                                        .property("name")
                                        .property("name")
                                        .build())
                        .build())
                .build();
        Relationship rel = knows().alias("F")
                .alias("F")
                .property("since", property(Neo4jType.DATE).alias("from").alias("from").build())
                .constraint(
                        "u",
                        RelationshipConstraint.builder()
                                .type(ConstraintType.UNIQUE)
                                .property("since")
                                .property("since")
                                .build())
                .extensions(RelationshipExtensions.builder()
                        .index(
                                "i",
                                RelationshipIndex.builder()
                                        .type(IndexType.RANGE)
                                        .property("since")
                                        .property("since")
                                        .build())
                        .build())
                .build();
        Table table = Table.builder()
                .source("local")
                .column(TableColumn.builder()
                        .name("a")
                        .supportedType(Neo4jType.STRING)
                        .supportedType(Neo4jType.STRING)
                        .build())
                .build();
        Mapping nodeMapping = NodeMapping.builder()
                .node("person")
                .table("t")
                .keyProperty("id")
                .keyProperty("id")
                .build();
        Mapping relMapping = RelationshipMapping.builder()
                .relationship("knows")
                .table("t")
                .fromNode(TargetMapping.builder().build())
                .toNode(TargetMapping.builder().build())
                .keyProperty("since")
                .keyProperty("since")
                .build();
        GraphModel model = model().node("person", person)
                .relationship("knows", rel)
                .extensions(GraphModelExtensions.builder()
                        .dataSourceTable("people", table)
                        .dataSourceMapping("load", List.of(nodeMapping, relMapping))
                        .build())
                .build();
        List<Issue> issues = CoreValidators.REPEATED_ENTRIES.validate(model);
        assertEquals(14, issues.size(), issues.toString());
        assertIssue(issues, "duplicate_node_alias", "nodes.person.aliases[1]", Map.of("value", "Human"));
        assertIssue(issues, "duplicate_node_implied_label", "nodes.person.labels.implied[1]", Map.of("value", "Agent"));
        assertIssue(issues, "duplicate_node_optional_label", "nodes.person.labels.optional[1]", Map.of("value", "Actor"));
        assertIssue(issues, "duplicate_node_property_alias", "nodes.person.properties.name.aliases[2]", Map.of("value", "x"));
        assertIssue(issues, "duplicate_node_constraint_property", "nodes.person.constraints.u.properties[1]", Map.of("value", "name"));
        assertIssue(issues, "duplicate_node_index_label", "nodes.person.extensions.indexes.i.labels[1]", Map.of("value", "Person"));
        assertIssue(issues, "duplicate_node_index_property", "nodes.person.extensions.indexes.i.properties[1]", Map.of("value", "name"));
        assertIssue(issues, "duplicate_relationship_alias", "relationships.knows.aliases[1]", Map.of("value", "F"));
        assertIssue(issues, "duplicate_relationship_property_alias", "relationships.knows.properties.since.aliases[1]", Map.of("value", "from"));
        assertIssue(issues, "duplicate_relationship_constraint_property", "relationships.knows.constraints.u.properties[1]", Map.of("value", "since"));
        assertIssue(issues, "duplicate_relationship_index_property", "relationships.knows.extensions.indexes.i.properties[1]", Map.of("value", "since"));
        assertIssue(issues, "duplicate_table_column_supported_type", "extensions.data_source_tables.people.columns[0].supported[1]", Map.of("value", "STRING"));
        assertIssue(issues, "duplicate_node_mapping_key", "extensions.data_source_mappings.load[0].key[1]", Map.of("value", "id"));
        assertIssue(issues, "duplicate_relationship_mapping_key", "extensions.data_source_mappings.load[1].key[1]", Map.of("value", "since"));
    }

    @Test
    void repeatedEntryIsReportedForEachRepeat() {
        Node person = Node.builder().alias("a").alias("a").alias("a").build();
        List<Issue> issues = CoreValidators.REPEATED_ENTRIES.validate(model().node("person", person).build());
        assertEquals(2, issues.size());
        assertEquals("nodes.person.aliases[1]", issues.get(0).path().get());
        assertEquals("nodes.person.aliases[2]", issues.get(1).path().get());
    }

    @Test
    void lengths() {
        String name257 = repeat('n', 257);
        String name256 = repeat('n', 256);
        String name801 = repeat('c', 801);
        String description1025 = repeat('d', 1025);
        Node person = Node.builder()
                .description(description1025)
                .property("a", property(Neo4jType.STRING).name(name257).description(description1025).build())
                .property("b", property(Neo4jType.STRING).name(name256).build())
                .constraint("c", NodeConstraint.builder().type(ConstraintType.KEY).property("a").name(name801).build())
                .extensions(NodeExtensions.builder()
                        .index("i", NodeIndex.builder().type(IndexType.RANGE).property("a").name(name801).build())
                        .build())
                .build();
        Relationship rel = knows().description(repeat('d', 1024))
                .constraint("c", RelationshipConstraint.builder().type(ConstraintType.UNIQUE).name(name801).build())
                .extensions(RelationshipExtensions.builder()
                        .index("i", RelationshipIndex.builder().type(IndexType.RANGE).name(repeat('c', 800)).build())
                        .build())
                .build();
        GraphModel model = model().name(name257)
                .description(repeat('d', 65_537))
                .node("person", person)
                .relationship("knows", rel)
                .build();
        List<Issue> issues = CoreValidators.LENGTHS.validate(model);
        assertEquals(8, issues.size(), issues.toString());
        assertIssue(issues, "name_too_long", "name", Map.of("length", "257", "max", "256"));
        assertIssue(issues, "description_too_long", "description", Map.of("length", "65537", "max", "65536"));
        assertIssue(issues, "description_too_long", "nodes.person.description", Map.of("length", "1025", "max", "1024"));
        assertIssue(issues, "name_too_long", "nodes.person.properties.a.name", Map.of("length", "257", "max", "256"));
        assertIssue(issues, "description_too_long", "nodes.person.properties.a.description", Map.of("length", "1025", "max", "1024"));
        assertIssue(issues, "name_too_long", "nodes.person.constraints.c.name", Map.of("length", "801", "max", "800"));
        assertIssue(issues, "name_too_long", "nodes.person.extensions.indexes.i.name", Map.of("length", "801", "max", "800"));
        assertIssue(issues, "name_too_long", "relationships.knows.constraints.c.name", Map.of("length", "801", "max", "800"));
        assertTrue(issues.stream().noneMatch(i -> i.path().get().startsWith("relationships.knows.extensions")));
    }

    @Test
    void lengthsCountCodePoints() {
        // 256 emoji are 512 UTF-16 chars but 256 code points
        String emoji = "😀".repeat(256);
        assertEquals(List.of(), CoreValidators.LENGTHS.validate(model().name(emoji).build()));
        assertEquals(1, CoreValidators.LENGTHS.validate(model().name(emoji + "x").build()).size());
    }

    @Test
    void coreIsSharedAndHasEveryValidator() {
        assertSame(Validators.core(), Validators.core());
        assertEquals(8, Validators.core().size());
        assertTrue(Validators.core().validators().contains(CoreValidators.REFERENCES));
        assertEquals("ReferenceValidator", CoreValidators.REFERENCES.toString());
        assertEquals(Validators.core(), ValidatorSet.of(Validators.core().validators()));
    }

    // ---- engine ----

    private static Validator issuing(String code, List<String> log, String name) {
        return new Validator() {
            @Override
            public List<Issue> validate(GraphModel model) {
                log.add(name);
                return List.of(Issue.of(code, "problem " + code, "p"));
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static Validator clean(List<String> log, String name, Validator... dependencies) {
        return new Validator() {
            @Override
            public List<Issue> validate(GraphModel model) {
                log.add(name);
                return List.of();
            }

            @Override
            public Set<Validator> dependsOn() {
                return Set.of(dependencies);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static Validator dependent(Validator dependency, List<String> log, String name) {
        return clean(log, name, dependency);
    }

    @Test
    void customLambdaCombinesWithCore() {
        GraphModel model = model().node("person", Node.builder().alias("a").alias("a").build())
                .build();
        Validator custom = m -> List.of(Issue.of("custom", "custom issue", "nodes"));

        List<Issue> viaSet = Validators.core().and(custom).validate(model);
        assertEquals(List.of("duplicate_node_alias", "custom"), codes(viaSet));

        List<Issue> viaSpec = GraphSpec.defaults().validate(model, Validators.core(), custom);
        assertEquals(codes(viaSet), codes(viaSpec));

        List<Issue> viaSets = Validators.core().and(ValidatorSet.of(custom)).validate(model);
        assertEquals(codes(viaSet), codes(viaSets));
    }

    @Test
    void resultIsUnmodifiable() {
        List<Issue> issues = Validators.core().validate(model().build());
        assertThrows(UnsupportedOperationException.class, () -> issues.add(Issue.of("a", "b", "c")));
        GraphModel bad = model().schema("x").build();
        assertThrows(UnsupportedOperationException.class, () -> core(bad).clear());
    }

    @Test
    void validatorsRunOnceEvenIfRepeated() {
        List<String> log = new ArrayList<>();
        Validator a = issuing("a", log, "a");
        ValidatorSet set = ValidatorSet.of(a, ValidatorSet.of(a), a);
        List<Issue> issues = GraphSpec.defaults().validate(model().build(), set, a, Validators.core());
        assertEquals(1, issues.size());
        assertEquals(List.of("a"), log);
    }

    @Test
    void dependenciesRunImplicitlyAndFirst() {
        List<String> log = new ArrayList<>();
        Validator base = clean(log, "base");
        Validator top = dependent(base, log, "top");
        ValidatorSet.of(top).validate(model().build());
        assertEquals(List.of("base", "top"), log);

        log.clear();
        Validator other = clean(log, "other");
        ValidatorSet.of(top, other, base).validate(model().build());
        assertEquals(List.of("base", "top", "other"), log);
    }

    @Test
    void dependentIsSkippedWhenDependencyHasIssues() {
        List<String> log = new ArrayList<>();
        Validator base = issuing("base", log, "base");
        Validator middle = dependent(base, log, "middle");
        Validator top = dependent(middle, log, "top");
        Validator independent = issuing("independent", log, "independent");

        List<Issue> issues = ValidatorSet.of(top, middle, independent).validate(model().build());
        assertEquals(List.of("base", "independent"), codes(issues));
        assertEquals(List.of("base", "independent"), log);
    }

    @Test
    void dependentRunsWhenDependenciesAreClean() {
        List<String> log = new ArrayList<>();
        Validator base = clean(log, "base");
        Validator top = new Validator() {
            @Override
            public List<Issue> validate(GraphModel model) {
                log.add("top");
                return List.of(Issue.of("top", "top issue", "p"));
            }

            @Override
            public Set<Validator> dependsOn() {
                return Set.of(base);
            }
        };
        assertEquals(List.of("top"), codes(ValidatorSet.of(top).validate(model().build())));
        assertEquals(List.of("base", "top"), log);
    }

    @Test
    void dependencyOnNestedSetFlattens() {
        List<String> log = new ArrayList<>();
        Validator a = issuing("a", log, "a");
        Validator b = clean(log, "b");
        Validator top = clean(log, "top", ValidatorSet.of(a, b));
        List<Issue> issues = ValidatorSet.of(top).validate(model().build());
        assertEquals(List.of("a"), codes(issues));
        assertEquals(List.of("a", "b"), log);
    }

    @Test
    void cycleIsDetected() {
        Validator[] holder = new Validator[2];
        holder[0] = new Validator() {
            @Override
            public List<Issue> validate(GraphModel model) {
                return List.of();
            }

            @Override
            public Set<Validator> dependsOn() {
                return Set.of(holder[1]);
            }
        };
        holder[1] = new Validator() {
            @Override
            public List<Issue> validate(GraphModel model) {
                return List.of();
            }

            @Override
            public Set<Validator> dependsOn() {
                return Set.of(holder[0]);
            }
        };
        ValidatorSet set = ValidatorSet.of(holder[0]);
        assertThrows(IllegalStateException.class, () -> set.validate(model().build()));
    }

    @Test
    void nestedSetsFlattenInOrder() {
        List<String> log = new ArrayList<>();
        Validator a = clean(log, "a");
        Validator b = clean(log, "b");
        Validator c = clean(log, "c");
        ValidatorSet nested = ValidatorSet.of(a, ValidatorSet.of(b, ValidatorSet.of(c)));
        assertEquals(List.of(a, b, c), nested.validators());
        nested.validate(model().build());
        assertEquals(List.of("a", "b", "c"), log);
    }

    // ---- labelled, consistent schema, well constrained and all ----

    @Test
    void setsAreSharedAndHaveEveryValidator() {
        assertSame(Validators.labelled(), Validators.labelled());
        assertSame(Validators.consistentSchema(), Validators.consistentSchema());
        assertSame(Validators.wellConstrained(), Validators.wellConstrained());
        assertSame(Validators.all(), Validators.all());
        assertEquals(3, Validators.labelled().size());
        assertEquals(9, Validators.consistentSchema().size());
        assertEquals(3, Validators.wellConstrained().size());
        assertEquals(ValidatorSet.of(LabelledValidators.all()), Validators.labelled());
        assertEquals(ValidatorSet.of(ConsistentSchemaValidators.all()), Validators.consistentSchema());
        assertEquals(ValidatorSet.of(WellConstrainedValidators.all()), Validators.wellConstrained());
        assertEquals("NodeIdentifierLabelValidator", LabelledValidators.NODE_IDENTIFIER_LABELS.toString());
        assertEquals("DuplicateIndexValidator", ConsistentSchemaValidators.DUPLICATE_INDEXES.toString());
        assertEquals("PropertyTypeValidator", WellConstrainedValidators.PROPERTY_TYPES.toString());
    }

    @Test
    void allContainsEachSetInOrder() {
        List<Validator> expected = new ArrayList<>(Validators.core().validators());
        expected.addAll(Validators.labelled().validators());
        expected.addAll(Validators.consistentSchema().validators());
        expected.addAll(Validators.wellConstrained().validators());
        assertEquals(expected, Validators.all().validators());
        assertEquals(Validators.core().and(Validators.labelled()).and(Validators.consistentSchema()).and(Validators.wellConstrained()), Validators.all());
    }

    @Test
    void consistentSchemaValidatorsDependOnCore() {
        for (Validator validator : Validators.consistentSchema()) {
            assertEquals(
                    Set.of(
                            CoreValidators.REFERENCES,
                            CoreValidators.CONSTRAINT_PROPERTY_COUNTS,
                            CoreValidators.INDEX_OPTIONS),
                    validator.dependsOn(),
                    validator.toString());
        }
        for (Validator validator : Validators.labelled()) {
            assertEquals(Set.of(), validator.dependsOn(), validator.toString());
        }
        for (Validator validator : Validators.wellConstrained()) {
            assertEquals(Set.of(), validator.dependsOn(), validator.toString());
        }
    }

    @Test
    void consistentSchemaIsSkippedWhenCoreReferencesFail() {
        Node person = Node.builder()
                .labels(Labels.builder().identifier("Person").build())
                .property("name", property(Neo4jType.STRING).build())
                .constraint(
                        "ghost",
                        NodeConstraint.builder()
                                .type(ConstraintType.EXISTS)
                                .property("missing")
                                .build())
                .build();
        GraphModel model = model().node("person", person).build();
        List<Issue> issues = Validators.consistentSchema().validate(model);
        assertEquals(List.of("missing_node_constraint_property"), codes(issues));
        // the checks themselves find the unset name when run without their dependencies' result mattering
        assertEquals(
                List.of("unset_node_constraint_name"),
                codes(ConsistentSchemaValidators.SCHEMA_NAMES.validate(model)));
    }

    @Test
    void consistentSchemaRunsOnceCoreIsClean() {
        Node person = Node.builder()
                .labels(Labels.builder().identifier("Person").build())
                .property("name", property(Neo4jType.STRING).build())
                .constraint(
                        "unnamed",
                        NodeConstraint.builder()
                                .type(ConstraintType.EXISTS)
                                .property("name")
                                .build())
                .build();
        List<Issue> issues = Validators.consistentSchema().validate(model().node("person", person).build());
        assertEquals(1, issues.size(), issues.toString());
        assertIssue(issues, "unset_node_constraint_name", "nodes.person.constraints.unnamed.name", Map.of());
    }

    @Test
    void labelledAndWellConstrainedRunDespiteCoreIssues() {
        GraphModel model = model().schema("x").node("nothing", Node.builder().build()).build();
        assertEquals(
                List.of("unknown_schema", "unset_node_identifier_label"),
                codes(Validators.core().and(Validators.labelled()).validate(model)));
        assertEquals(
                List.of("node_without_properties", "node_without_constraint"),
                codes(Validators.wellConstrained().validate(model)));
    }

    @Test
    void allFindsIssuesOfEverySet() {
        Node nothing = Node.builder().build();
        GraphModel model = model().schema("x").node("nothing", nothing).build();
        assertEquals(
                List.of(
                        "unknown_schema",
                        "unset_node_identifier_label",
                        "node_without_properties",
                        "node_without_constraint"),
                codes(Validators.all().validate(model)));
    }

    @Test
    void duplicateSchemaNamesShareOneNamespaceAcrossOwners() {
        Node person = Node.builder()
                .property("name", property(Neo4jType.STRING).build())
                .constraint(
                        "c",
                        NodeConstraint.builder()
                                .type(ConstraintType.EXISTS)
                                .property("name")
                                .name("shared")
                                .build())
                .build();
        Relationship rel = knows().property("since", property(Neo4jType.DATE).build())
                .extensions(RelationshipExtensions.builder()
                        .index(
                                "i",
                                RelationshipIndex.builder()
                                        .type(IndexType.RANGE)
                                        .property("since")
                                        .name("shared")
                                        .build())
                        .build())
                .build();
        List<Issue> issues = ConsistentSchemaValidators.UNIQUE_SCHEMA_NAMES.validate(
                model().node("person", person).relationship("knows", rel).build());
        assertEquals(1, issues.size(), issues.toString());
        assertIssue(
                issues,
                "duplicate_schema_name",
                "relationships.knows.extensions.indexes.i.name",
                Map.of("value", "shared", "first", "nodes.person.constraints.c.name"));
    }

    private static List<String> codes(List<Issue> issues) {
        return issues.stream().map(Issue::code).collect(Collectors.toList());
    }
}
