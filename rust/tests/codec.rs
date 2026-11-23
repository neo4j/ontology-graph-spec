// Tests for the codec surface beyond the spec tests: format detection, the compact JSON form,
// converting YAML to the JSON value it holds, the model as a JSON value, and — with the
// `schema-validation` feature — the structural gate against the published JSON Schema.

use ontology_graph_spec::codec::{
    detect_document_format, encode_json_compact, model_to_json_value, yaml_to_json_value,
};
use ontology_graph_spec::GraphSpec;

const MODEL: &str = r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","nodes":{"person":{"label":"Person","properties":{"name":{"type":"STRING"}}}}}"#;

#[test]
fn decode_auto_detects_the_syntax() {
    // The headline API: one call, no format check.
    let model = GraphSpec::decode(MODEL).unwrap();
    let yaml = concat!(
        "schema: https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json\n",
        "version: \"1\"\n",
        "id: t\n",
        "nodes:\n",
        "  person:\n",
        "    label: Person\n",
        "    properties:\n",
        "      name: { type: STRING }\n",
    );
    assert_eq!(model, GraphSpec::decode(yaml).unwrap());
    // The pinned escape hatches give the same model.
    assert_eq!(model, ontology_graph_spec::decode_json(MODEL).unwrap());
    assert_eq!(model, ontology_graph_spec::decode_yaml(yaml).unwrap());
    // Detect reports the syntax for callers that need it (e.g. encoding back).
    assert_eq!(detect_document_format(MODEL), GraphSpec::Json);
    assert_eq!(detect_document_format(yaml), GraphSpec::Yaml);
}

#[test]
fn a_strict_json_parse_decides_the_syntax() {
    assert_eq!(detect_document_format(MODEL), GraphSpec::Json);
    // YAML that looks JSON-ish, with a JSON syntax error, is YAML.
    assert_eq!(
        detect_document_format(concat!(
            "schema: https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json\n",
            "version: \"1\"\n",
            "id: t\n",
        )),
        GraphSpec::Yaml
    );
    assert_eq!(detect_document_format("{broken"), GraphSpec::Yaml);
}

#[test]
fn the_model_encodes_as_a_json_value_of_its_document() {
    let model = GraphSpec::decode(MODEL).unwrap();
    let value = model_to_json_value(&model).unwrap();
    // The longhand form, in document order, with the shorthand expanded.
    assert_eq!(
        value,
        serde_json::json!({
            "schema": "https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json",
            "version": "1",
            "id": "t",
            "nodes": {"person": {"labels": {"identifier": "Person"}, "properties": {"name": {"type": "STRING"}}}}
        })
    );
    let compact = encode_json_compact(&model).unwrap();
    assert_eq!(compact, value.to_string());
    let _ = GraphSpec::Json.encode(&model).unwrap();
}

#[test]
fn compact_json_is_the_pretty_json_without_the_layout() {
    let model = GraphSpec::decode(MODEL).unwrap();
    let pretty = GraphSpec::Json.encode(&model).unwrap();
    let compact = encode_json_compact(&model).unwrap();
    assert_eq!(
        serde_json::from_str::<serde_json::Value>(&pretty).unwrap(),
        serde_json::from_str::<serde_json::Value>(&compact).unwrap()
    );
    assert!(!compact.contains('\n'));
    assert_eq!(
        compact,
        r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","nodes":{"person":{"labels":{"identifier":"Person"},"properties":{"name":{"type":"STRING"}}}}}"#
    );
}

#[test]
fn yaml_holds_the_same_json_value_the_model_decodes_from() {
    let yaml = concat!(
        "schema: https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json\n",
        "version: \"1\"\n",
        "id: t\n",
        "nodes:\n",
        "  person:\n",
        "    label: Person\n",
        "    properties:\n",
        "      name: { type: STRING }\n",
    );
    let value = yaml_to_json_value(yaml).unwrap();
    let model = GraphSpec::decode(yaml).unwrap();
    assert_eq!(
        value,
        serde_json::json!({
            "schema": "https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json",
            "version": "1",
            "id": "t",
            "nodes": {"person": {"label": "Person", "properties": {"name": {"type": "STRING"}}}}
        })
    );
    // The document order survives, and the shorthand expanded by the model is still there in
    // the value: the value is the document, the model is the longhand form.
    assert_eq!(
        value["nodes"]["person"]
            .as_object()
            .unwrap()
            .keys()
            .collect::<Vec<_>>(),
        ["label", "properties"]
    );
    assert_eq!(
        model.nodes["person"].labels.identifier,
        Some("Person".to_owned())
    );
}

#[test]
fn yaml_to_json_reads_plain_scalars_like_the_model_does() {
    let yaml = "identifier: Null\nother: true\n";
    let value = yaml_to_json_value(yaml).unwrap();
    assert_eq!(value["identifier"], serde_json::json!("Null"));
    assert_eq!(value["other"], serde_json::json!(true));
}

#[test]
fn a_float_index_option_keeps_its_shortest_spelling() {
    // 1.1 as f32 widened to f64 is 1.100000023841858; the reference writes its Float as "1.1".
    let json = r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","nodes":{"person":{"label":"Person","properties":{"embedding":{"type":"VECTOR<FLOAT32>","dimension":4}},"extensions":{"indexes":{"vec":{"type":"VECTOR","labels":["Person"],"properties":["embedding"],"options":{"type":"VECTOR","vector.default_search_expansion_factor":1.1}}}}}}}"#;
    let model = GraphSpec::decode(json).unwrap();
    let encoded = GraphSpec::Json.encode(&model).unwrap();
    assert!(
        encoded.contains("\"vector.default_search_expansion_factor\": 1.1"),
        "{encoded}"
    );
    let yaml = GraphSpec::Yaml.encode(&model).unwrap();
    assert!(
        yaml.contains("vector.default_search_expansion_factor: 1.1"),
        "{yaml}"
    );
    // And it decodes back to the same model.
    assert_eq!(GraphSpec::decode(&encoded).unwrap(), model);
    assert_eq!(GraphSpec::decode(&yaml).unwrap(), model);
}

#[test]
fn an_empty_map_in_a_custom_list_stays_a_map() {
    let json = r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","extensions":{"custom":{"items":[{}]}}}"#;
    let model = GraphSpec::decode(json).unwrap();
    let yaml = GraphSpec::Yaml.encode(&model).unwrap();
    assert!(yaml.contains("- {}"), "{yaml}");
    assert_eq!(GraphSpec::decode(&yaml).unwrap(), model);
}

#[test]
fn control_characters_round_trip_through_yaml_escapes() {
    let json = "{\"schema\":\"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json\",\"version\":\"1\",\"id\":\"t\",\"description\":\"a\\b\\f gone\",\"extensions\":{\"custom\":{\"key\\b\":\"v\"}}}";
    let model = GraphSpec::decode(json).unwrap();
    let yaml = GraphSpec::Yaml.encode(&model).unwrap();
    assert!(yaml.contains(r#""a\b\f gone""#), "{yaml}");
    assert!(yaml.contains(r#""key\b": "v""#), "{yaml}");
    assert_eq!(GraphSpec::decode(&yaml).unwrap(), model);
}

#[test]
fn non_finite_floats_fail_to_encode_instead_of_becoming_null() {
    // A custom value holding NaN: serde_json would silently write null, which decodes back as an
    // error. Fail with a useful message instead, in every encoder.
    let json = r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","extensions":{"custom":{"x":1.0}}}"#;
    let mut model = GraphSpec::decode(json).unwrap();
    model.extensions.custom.insert(
        "x".into(),
        ontology_graph_spec::model::custom::CustomValue::Float(f64::NAN),
    );
    for spec in [GraphSpec::Json, GraphSpec::Yaml] {
        let error = spec.encode(&model).unwrap_err();
        assert!(error.to_string().contains("non-finite float"), "{error}");
    }

    // A non-finite index coordinate fails the same way.
    let json = r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","nodes":{"place":{"label":"Place","properties":{"location":{"type":"POINT"}},"extensions":{"indexes":{"geo":{"type":"POINT","labels":["Place"],"properties":["location"],"options":{"type":"POINT"}}}}}}}"#;
    let mut model = GraphSpec::decode(json).unwrap();
    let indexes = &mut model.nodes.get_mut("place").unwrap().extensions.indexes;
    let options = indexes.get_mut("geo").unwrap().options.as_mut().unwrap();
    let ontology_graph_spec::model::index_option::IndexOption::Point(point) = options else {
        panic!("point options")
    };
    point.wgs84_min[0] = f64::INFINITY;
    let error = GraphSpec::Json.encode(&model).unwrap_err();
    assert!(error.to_string().contains("non-finite float"), "{error}");
}

#[test]
fn an_escaped_enum_value_decodes() {
    // STR\u0049NG is STRING: JSON escapes must not break enum decoding.
    let json = r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","nodes":{"person":{"label":"Person","properties":{"name":{"type":"STR\u0049NG"}}}}}"#;
    let model = GraphSpec::decode(json).unwrap();
    assert_eq!(
        model.nodes["person"].properties["name"].r#type.to_string(),
        "STRING"
    );
    // And from an owned Value, where no borrow is possible at all.
    let value: serde_json::Value = serde_json::from_str(json).unwrap();
    let model: ontology_graph_spec::GraphModel = serde_json::from_value(value).unwrap();
    assert_eq!(
        model.nodes["person"].properties["name"].r#type.to_string(),
        "STRING"
    );
}

#[cfg(feature = "schema-validation")]
mod schema_validation {
    use super::*;
    use ontology_graph_spec::codec::validate_document_schema;

    #[test]
    fn accepts_a_valid_document() {
        let value: serde_json::Value = serde_json::from_str(MODEL).unwrap();
        assert!(validate_document_schema(&value).is_ok());
    }

    #[test]
    fn rejects_unknown_keys_and_bad_shapes() {
        // The schema closes every object: an unknown key is a violation, where the model decode
        // ignores it, as the reference implementation does.
        let value: serde_json::Value = serde_json::from_str(
            r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","nodes":{"person":{"labell":"Person"}}}"#,
        )
        .unwrap();
        let errors = validate_document_schema(&value).unwrap_err();
        assert!(
            errors.0.iter().any(|error| error.contains("labell")),
            "{errors:?}"
        );

        // A required root field missing.
        let value: serde_json::Value =
            serde_json::from_str(r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1"}"#)
                .unwrap();
        assert!(!validate_document_schema(&value).unwrap_err().0.is_empty());

        // An unknown constraint type.
        let value: serde_json::Value = serde_json::from_str(
            r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t","nodes":{"person":{"constraints":{"c":{"type":"EXISTS_ALWAYS","properties":["x"]}}}}}"#,
        )
        .unwrap();
        let errors = validate_document_schema(&value).unwrap_err();
        assert!(
            errors.0.iter().any(|error| error.contains("EXISTS_ALWAYS")),
            "{errors:?}"
        );
    }

    #[test]
    fn the_yaml_syntax_validates_through_the_json_value() {
        let yaml = yaml_to_json_value(
            "schema: https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json\nversion: \"1\"\nid: t\n",
        )
        .unwrap();
        assert!(validate_document_schema(&yaml).is_ok());
    }
}
