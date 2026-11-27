// Tests for the validators the spec test cases don't reach: every set beyond `core`, the schema
// url check, and the dependency tree that decides which validators run.
//
// Each case builds the smallest model that triggers the issue, mirroring how the spec test cases
// probe the `core` set, and asserts the issue codes and paths the reference implementation emits.

use ontology_graph_spec::model::GraphModel;
use ontology_graph_spec::validate::{Issue, ValidationSet};

/// Decodes a model with the given body: the standard schema, version and id wrapped around
/// whatever the case needs. The body is a JSON object of the model's content, e.g.
/// `{"nodes": {...}}`.
fn model(body: &str) -> GraphModel {
    let inner = &body[body.find('{').unwrap() + 1..body.rfind('}').unwrap()];
    let json = format!(
        "{{\"schema\":\"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json\",\
         \"version\":\"1\",\"id\":\"test\",{inner}}}"
    );
    ontology_graph_spec::GraphSpec::decode(&json)
        .unwrap_or_else(|error| panic!("decoding model: {error}\n{json}"))
}

/// Decodes a model as written, header included.
fn raw_model(json: &str) -> GraphModel {
    ontology_graph_spec::GraphSpec::decode(json)
        .unwrap_or_else(|error| panic!("decoding model: {error}\n{json}"))
}

/// The codes and paths of the issues, in order, for asserting.
fn codes_and_paths(issues: &[Issue]) -> Vec<(String, String)> {
    issues
        .iter()
        .map(|issue| {
            (
                issue.code.clone(),
                issue.path.clone().unwrap_or_else(|| "<null>".to_owned()),
            )
        })
        .collect()
}

fn validate(body: &str, set: &ValidationSet) -> Vec<(String, String)> {
    codes_and_paths(&model(body).validate(set))
}

#[test]
fn schema_url() {
    // A document with an unrecognized schema url fails to decode, as it does in the reference
    // implementation; the validator catches the case where such a model is built in code.
    let issues = GraphModel {
        schema: "https://example.com/schema.json".to_owned(),
        version: "1".to_owned(),
        id: "t".to_owned(),
        ..Default::default()
    }
    .validate(&ValidationSet::core());
    assert_eq!(
        codes_and_paths(&issues),
        [("invalid_schema_url".to_owned(), "/schema".to_owned())]
    );

    // Surrounding whitespace, trailing slashes and a missing schema.json are tolerated, and
    // documents from older released versions are read at the latest one, their schema re-stamped.
    for (schema, expected) in [
        (
            " https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json ",
            None,
        ),
        ("https://neo4j.com/docs/ontology-graph-spec/1.1.0/", None),
        ("https://neo4j.com/docs/ontology-graph-spec/1.1.0", None),
        (
            "https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json",
            Some("https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json"),
        ),
    ] {
        let json = format!("{{\"schema\":\"{schema}\",\"version\":\"1\",\"id\":\"t\"}}");
        let model = raw_model(&json);
        assert_eq!(model.schema, expected.unwrap_or(schema));
        assert!(
            model.validate(&ValidationSet::core()).is_empty(),
            "no issues for {schema}"
        );
    }
}

#[test]
fn labelled() {
    assert_eq!(
        validate("{\"nodes\":{\"person\":{}}}", &ValidationSet::labelled()),
        [(
            "missing_node_identifier_label".to_owned(),
            "/nodes/person/labels/identifier".to_owned()
        )]
    );

    assert_eq!(
        validate(
            "{\"relationships\":{\"knows\":{\"type\":\"  \",\"from\":{},\"to\":{}}}}",
            &ValidationSet::labelled()
        ),
        [
            (
                "missing_relationship_type".to_owned(),
                "/relationships/knows/type".to_owned()
            ),
            (
                "missing_relationship_from".to_owned(),
                "/relationships/knows/from/node".to_owned()
            ),
            (
                "missing_relationship_to".to_owned(),
                "/relationships/knows/to/node".to_owned()
            ),
        ]
    );
}

#[test]
fn consistent_schema() {
    // Empty and duplicate property names on nodes and relationships.
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},\"properties\":{
                \"a\":{\"type\":\"STRING\",\"name\":\"same\"},
                \"b\":{\"type\":\"STRING\",\"name\":\"same\"},
                \"c\":{\"type\":\"STRING\"}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [
            (
                "missing_property_name".to_owned(),
                "/nodes/person/properties/c/name".to_owned()
            ),
            (
                "duplicate_property_name".to_owned(),
                "/nodes/person/properties/a/name".to_owned()
            ),
            (
                "duplicate_property_name".to_owned(),
                "/nodes/person/properties/b/name".to_owned()
            ),
        ]
    );

    assert_eq!(
        validate(
            "{\"relationships\":{\"knows\":{\"type\":\"KNOWS\",\"from\":{},\"to\":{},\"properties\":{
                \"a\":{\"type\":\"STRING\"},\"b\":{\"type\":\"STRING\",\"name\":\"x\"},\"c\":{\"type\":\"STRING\",\"name\":\"x\"}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [
            ("missing_property_name".to_owned(), "/relationships/knows/properties/a/name".to_owned()),
            ("duplicate_property_name".to_owned(), "/relationships/knows/properties/b/name".to_owned()),
            ("duplicate_property_name".to_owned(), "/relationships/knows/properties/c/name".to_owned()),
        ]
    );

    // Indexes and constraints without properties. This set requires every property to carry a
    // name, so the properties are named.
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},
                \"properties\":{\"name\":{\"type\":\"STRING\",\"name\":\"name\"}},
                \"extensions\":{\"indexes\":{\"empty\":{\"type\":\"RANGE\",\"labels\":[\"Person\"],\"properties\":[]}}},
                \"constraints\":{\"none\":{\"type\":\"KEY\",\"properties\":[]}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [
            ("missing_node_index_properties".to_owned(), "/nodes/person/extensions/indexes/empty/properties".to_owned()),
            ("missing_node_constraint_properties".to_owned(), "/nodes/person/constraints/none/properties".to_owned()),
        ]
    );

    assert_eq!(
        validate(
            "{\"relationships\":{\"knows\":{\"type\":\"KNOWS\",\"from\":{},\"to\":{},
                \"properties\":{\"since\":{\"type\":\"DATE\",\"name\":\"since\"}},
                \"extensions\":{\"indexes\":{\"empty\":{\"type\":\"RANGE\",\"properties\":[]}}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [(
            "missing_relationship_index_properties".to_owned(),
            "/relationships/knows/extensions/indexes/empty/properties".to_owned()
        )]
    );

    // Duplicated constraint property sets: same type on the same properties, or any type when
    // composite.
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},\"properties\":{
                \"name\":{\"type\":\"STRING\",\"name\":\"name\"},\"age\":{\"type\":\"INTEGER\",\"name\":\"age\"}},
                \"constraints\":{
                    \"one\":{\"type\":\"UNIQUE\",\"properties\":[\"name\",\"age\"]},
                    \"two\":{\"type\":\"KEY\",\"properties\":[\"name\",\"age\"]}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [
            ("duplicate_node_constraint_property_set".to_owned(), "/nodes/person/constraints/one/properties".to_owned()),
            ("duplicate_node_constraint_property_set".to_owned(), "/nodes/person/constraints/two/properties".to_owned()),
        ]
    );

    assert_eq!(
        validate(
            "{\"relationships\":{\"knows\":{\"type\":\"KNOWS\",\"from\":{},\"to\":{},\"properties\":{
                \"since\":{\"type\":\"DATE\",\"name\":\"since\"},\"rating\":{\"type\":\"FLOAT\",\"name\":\"rating\"}},
                \"constraints\":{
                    \"one\":{\"type\":\"UNIQUE\",\"properties\":[\"since\",\"rating\"]},
                    \"two\":{\"type\":\"KEY\",\"properties\":[\"rating\",\"since\"]}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [
            ("duplicate_relationship_constraint_property_set".to_owned(), "/relationships/knows/constraints/one/properties".to_owned()),
            ("duplicate_relationship_constraint_property_set".to_owned(), "/relationships/knows/constraints/two/properties".to_owned()),
        ]
    );

    // An existence constraint conflicts with a composite constraint covering its property.
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},\"properties\":{
                \"name\":{\"type\":\"STRING\",\"name\":\"name\"},\"age\":{\"type\":\"INTEGER\",\"name\":\"age\"}},
                \"constraints\":{
                    \"exists_name\":{\"type\":\"EXISTS\",\"properties\":[\"name\"]},
                    \"composite\":{\"type\":\"KEY\",\"properties\":[\"name\",\"age\"]}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [(
            "node_existence_composite_conflict".to_owned(),
            "/nodes/person/constraints/exists_name".to_owned()
        )]
    );

    // Index and constraint names must be unique across the whole model.
    assert_eq!(
        validate(
            "{\"nodes\":{
                \"person\":{\"labels\":{\"identifier\":\"Person\"},\"properties\":{\"name\":{\"type\":\"STRING\",\"name\":\"name\"}},
                    \"constraints\":{\"by_name\":{\"type\":\"KEY\",\"properties\":[\"name\"],\"name\":\"shared\"}}},
                \"other\":{\"labels\":{\"identifier\":\"Other\"},\"properties\":{\"id\":{\"type\":\"STRING\",\"name\":\"id\"}},
                    \"extensions\":{\"indexes\":{\"by_id\":{\"type\":\"RANGE\",\"labels\":[\"Other\"],\"properties\":[\"id\"],\"name\":\"shared\"}}}}}}",
            &ValidationSet::consistent_schema()
        ),
        [
            ("duplicate_node_index_constraint_name".to_owned(), "/nodes/person/constraints/by_name/name".to_owned()),
            ("duplicate_node_index_constraint_name".to_owned(), "/nodes/other/extensions/indexes/by_id/name".to_owned()),
        ]
    );
}

#[test]
fn mappable() {
    // Node mappings without a key.
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},\"properties\":{\"id\":{\"type\":\"STRING\"}}}},
             \"extensions\":{\"data_source_tables\":{\"people\":{\"source\":\"local\",\"columns\":[{\"name\":\"id\"}]}},
             \"data_source_mappings\":{\"load\":[{\"type\":\"NODE\",\"node\":\"person\",\"table\":\"people\",
                 \"properties\":{\"id\":{\"column\":\"id\"}}}]}}}",
            &ValidationSet::mappable()
        ),
        [(
            "missing_node_mapping_key".to_owned(),
            "/extensions/data_source_mappings/load/0/key".to_owned()
        )]
    );

    // Mapping dependency problems: duplicate ids, unknown, self, and one in another list. The id
    // issues come first, then the dependency issues in mapping order.
    assert_eq!(
        validate(
            "{\"extensions\":{\"data_source_mappings\":{\"load\":[
                {\"type\":\"QUERY\",\"table\":\"t\",\"query\":\"select 1\",\"id\":\"dup\",\"depends_on\":\"gone\"},
                {\"type\":\"QUERY\",\"table\":\"t\",\"query\":\"select 2\",\"id\":\"dup\"},
                {\"type\":\"QUERY\",\"table\":\"t\",\"query\":\"select 3\",\"id\":\"self\",\"depends_on\":\"self\"},
                {\"type\":\"QUERY\",\"table\":\"t\",\"query\":\"select 4\",\"depends_on\":\"elsewhere\"}],
             \"other\":[{\"type\":\"QUERY\",\"table\":\"t\",\"query\":\"select 5\",\"id\":\"elsewhere\"}]}}}",
            &ValidationSet::mappable()
        ),
        [
            ("mapping_id_duplicate".to_owned(), "/extensions/data_source_mappings/load/1/id".to_owned()),
            ("mapping_depends_on_unknown".to_owned(), "/extensions/data_source_mappings/load/0/depends_on".to_owned()),
            ("mapping_depends_on_self".to_owned(), "/extensions/data_source_mappings/load/2/depends_on".to_owned()),
            ("mapping_depends_on_other_list".to_owned(), "/extensions/data_source_mappings/load/3/depends_on".to_owned()),
        ]
    );

    assert_eq!(
        validate(
            "{\"extensions\":{\"data_source_mappings\":{\"load\":[
                {\"type\":\"QUERY\",\"table\":\"t\",\"query\":\"select 1\",\"id\":\"a\",\"depends_on\":\"b\"},
                {\"type\":\"QUERY\",\"table\":\"t\",\"query\":\"select 2\",\"id\":\"b\",\"depends_on\":\"a\"}]}}}",
            &ValidationSet::mappable()
        ),
        [(
            "mapping_dependency_cycle".to_owned(),
            "/extensions/data_source_mappings/load/0/depends_on".to_owned()
        )]
    );

    // Table column names: missing, duplicated, and a missing type outside local tables.
    assert_eq!(
        validate(
            "{\"extensions\":{\"data_source_tables\":{
                \"cloud\":{\"source\":\"sql/postgres\",\"columns\":[
                    {\"name\":\"\"},{\"name\":\"dup\"},{\"name\":\"dup\",\"supported\":[\"STRING\"]}]},
                \"local\":{\"source\":\"local\",\"columns\":[{\"name\":\"\"},{\"name\":\"ok\"}]}}}}",
            &ValidationSet::mappable()
        ),
        [
            ("missing_table_column_name".to_owned(), "/extensions/data_source_tables/cloud/columns/0/name".to_owned()),
            ("missing_table_column_name".to_owned(), "/extensions/data_source_tables/local/columns/0/name".to_owned()),
            ("duplicate_table_column_name".to_owned(), "/extensions/data_source_tables/cloud/columns/1/name".to_owned()),
            ("duplicate_table_column_name".to_owned(), "/extensions/data_source_tables/cloud/columns/2/name".to_owned()),
            ("missing_table_column_supported_type".to_owned(), "/extensions/data_source_tables/cloud/columns/0/supported".to_owned()),
            ("missing_table_column_supported_type".to_owned(), "/extensions/data_source_tables/cloud/columns/1/supported".to_owned()),
        ]
    );
}

#[test]
fn bulk_importable() {
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Per:son\"},\"properties\":{
                \"id\":{\"type\":\"STRING\"},\"num\":{\"type\":\"FLOAT\"}}}},
             \"relationships\":{\"knows\":{\"type\":\"KN=OWS\",\"from\":{},\"to\":{}}},
             \"extensions\":{\"data_source_tables\":{\"people\":{\"source\":\"local\",\"columns\":[{\"name\":\"id\"}]}},
             \"data_source_mappings\":{\"load\":[{\"type\":\"NODE\",\"node\":\"person\",\"table\":\"people\",
                 \"properties\":{\"id\":{\"column\":\"id\"}},\"key\":[\"num\"]}]}}}",
            &ValidationSet::bulk_importable()
        ),
        [
            ("invalid_node_label_token".to_owned(), "/nodes/person/labels/identifier".to_owned()),
            ("invalid_relationship_type_token".to_owned(), "/relationships/knows/type".to_owned()),
            ("invalid_node_mapping_key_type".to_owned(), "/extensions/data_source_mappings/load/0/key/0".to_owned()),
        ]
    );

    // Composite constraint properties must be STRING or INTEGER for bulk import.
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},\"properties\":{
                \"name\":{\"type\":\"STRING\"},\"when\":{\"type\":\"DATE\"}},
                \"constraints\":{\"composite\":{\"type\":\"KEY\",\"properties\":[\"name\",\"when\"]}}}}}",
            &ValidationSet::bulk_importable()
        ),
        [(
            "invalid_node_composite_constraint_property_type".to_owned(),
            "/nodes/person/constraints/composite".to_owned()
        )]
    );
}

#[test]
fn well_constrained() {
    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},
                \"properties\":{\"name\":{\"type\":\"STRING\"}},
                \"constraints\":{\"name_key\":{\"type\":\"KEY\",\"properties\":[\"name\"]}}}},
             \"relationships\":{\"knows\":{\"type\":\"KNOWS\",\"from\":{},\"to\":{},
                \"properties\":{\"since\":{\"type\":\"DATE\"}},
                \"constraints\":{\"since_key\":{\"type\":\"KEY\",\"properties\":[\"since\"]},
                    \"since_exists\":{\"type\":\"EXISTS\",\"properties\":[\"since\"]}}}}}",
            &ValidationSet::well_constrained()
        ),
        [(
            "redundant_relationship_constraint_key_overlap".to_owned(),
            "/relationships/knows/constraints/since_exists".to_owned()
        ),]
    );

    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"}}}}",
            &ValidationSet::well_constrained()
        ),
        [
            (
                "missing_node_properties".to_owned(),
                "/nodes/person/properties".to_owned()
            ),
            (
                "missing_node_constraint_coverage".to_owned(),
                "/nodes/person/constraints".to_owned()
            ),
        ]
    );

    assert_eq!(
        validate(
            "{\"nodes\":{\"person\":{\"labels\":{\"identifier\":\"Person\"},\"properties\":{
                \"name\":{\"type\":\"STRING\"}},
                \"constraints\":{\"name_key\":{\"type\":\"KEY\",\"properties\":[\"name\"]},
                    \"name_exists\":{\"type\":\"EXISTS\",\"properties\":[\"name\"]}}}}}",
            &ValidationSet::well_constrained()
        ),
        [(
            "redundant_node_constraint_key_overlap".to_owned(),
            "/nodes/person/constraints/name_exists".to_owned()
        )]
    );
}

#[test]
fn dependent_validators_are_skipped_when_their_dependency_finds_issues() {
    // The type and existence constraint checks depend on the constraint property check: when that
    // finds a missing property, the dependent checks don't run, so their issues aren't repeated.
    let issues = model(
        "{\"nodes\":{\"person\":{\"properties\":{\"name\":{\"type\":\"STRING\"}},
            \"constraints\":{\"bad_type\":{\"type\":\"PROPERTY_TYPE\",\"properties\":[\"missing\"]}}}}}",
    )
    .validate(&ValidationSet::core());
    assert_eq!(
        codes_and_paths(&issues),
        [(
            "missing_node_constraint_property".to_owned(),
            "/nodes/person/constraints/bad_type/properties/0".to_owned()
        )]
    );
}

#[test]
fn all_covers_every_set() {
    // The union of every set flags issues from each of them on one model.
    let issues = model(
        "{\"nodes\":{\"person\":{
            \"properties\":{\"name\":{\"type\":\"STRING\"}},
            \"constraints\":{
                \"name_key\":{\"type\":\"KEY\",\"properties\":[\"name\"],\"name\":\"shared\"},
                \"name_unique\":{\"type\":\"UNIQUE\",\"properties\":[\"name\"],\"name\":\"shared\"}}}}}",
    )
    .validate(&ValidationSet::all());
    let codes: Vec<&str> = issues.iter().map(|issue| issue.code.as_str()).collect();
    // core
    assert!(
        codes.contains(&"duplicate_node_index_constraint_name"),
        "{codes:?}"
    );
    // labelled
    assert!(
        codes.contains(&"missing_node_identifier_label"),
        "{codes:?}"
    );
    // consistent_schema
    assert!(codes.contains(&"missing_property_name"), "{codes:?}");
    // well_constrained
    assert!(
        codes.contains(&"redundant_node_constraint_key_overlap"),
        "{codes:?}"
    );
    // Nothing else fires: the KEY and UNIQUE constraints cover the same single property, which
    // only the key overlap flags.
    assert_eq!(codes.len(), 5, "{codes:?}");
}
