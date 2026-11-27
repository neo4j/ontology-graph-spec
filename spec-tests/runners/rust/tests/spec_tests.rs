// Package spectests runs the Rust library against the spec tests under spec-tests/cases at the
// repository root, the same cases every other library runs.
//
// Documents must decode from every form to the same model, encode back to the canonical
// model.json (compared semantically) and model.yaml (compared byte for byte), and validate clean
// against the core set. Validation cases must report exactly the expected issues for every input.

use std::collections::BTreeMap;
use std::fs;
use std::path::{Path, PathBuf};

use ontology_graph_spec::model::GraphModel;
use ontology_graph_spec::validate::{Issue, ValidationSet};
use ontology_graph_spec::GraphSpec;

/// The cases directory, three levels up from this package.
const CASES: &str = "../../cases";

#[test]
fn documents() {
    for dir in case_dirs("documents") {
        let name = dir.file_name().unwrap().to_str().unwrap().to_owned();
        let json = read(&dir.join("model.json"));
        let yaml = read(&dir.join("model.yaml"));

        for file in document_files(&dir) {
            let model = decode(&file);
            let encoded_json = GraphSpec::Json.encode(&model).unwrap();
            assert_eq!(
                serde_json::from_str::<serde_json::Value>(&json).unwrap(),
                serde_json::from_str::<serde_json::Value>(&encoded_json).unwrap(),
                "{} encoded as JSON",
                file.display()
            );

            let encoded_yaml = GraphSpec::Yaml.encode(&model).unwrap();
            assert_eq!(
                yaml,
                encoded_yaml,
                "{} encoded as YAML (first difference at line {})",
                file.display(),
                first_difference(&yaml, &encoded_yaml)
            );
        }

        let model = GraphSpec::decode(&json).unwrap();
        let issues = model.validate(&ValidationSet::core());
        assert!(issues.is_empty(), "{name} core issues: {issues:#?}");
    }
}

#[test]
fn schema() {
    // Every JSON document in a documents case is valid against the published JSON Schema, as
    // the reference runner asserts.
    let mut count = 0;
    for dir in case_dirs("documents") {
        for file in document_files(&dir) {
            if file.extension().and_then(|e| e.to_str()) != Some("json") {
                continue;
            }
            let value: serde_json::Value = serde_json::from_str(&read(&file))
                .unwrap_or_else(|error| panic!("parsing {}: {error}", file.display()));
            ontology_graph_spec::codec::validate_document_schema(&value)
                .unwrap_or_else(|errors| panic!("{} schema errors: {errors:?}", file.display()));
            count += 1;
        }
    }
    assert!(count > 0, "found document cases to check");
}

#[test]
fn validate() {
    for set in case_dirs("validate") {
        for dir in case_dirs(&format!("validate/{}", set.display())) {
            let set = set.file_name().unwrap().to_str().unwrap();
            let validators = ValidationSet::by_name(set)
                .unwrap_or_else(|| panic!("unknown validation set '{set}'"));
            let name = format!("{set}/{}", dir.display());

            // The two input forms are equivalent: the JSON input encodes byte-exactly to the
            // YAML input.
            let yaml = read(&dir.join("input.yaml"));
            let encoded = GraphSpec::Yaml
                .encode(&decode(&dir.join("input.json")))
                .unwrap();
            assert_eq!(
                yaml,
                encoded,
                "{name} input.json encoded as YAML (first difference at line {})",
                first_difference(&yaml, &encoded)
            );

            let expected = expected_issues(&dir.join("issues.json"), &name);
            for input in [dir.join("input.json"), dir.join("input.yaml")] {
                let model = decode(&input);
                let issues = model.validate(&validators);
                for issue in &issues {
                    assert!(
                        !issue.message.trim().is_empty(),
                        "issue {} has a message",
                        issue.code
                    );
                    assert!(issue.path.is_some(), "issue {} has a path", issue.code);
                }
                let actual: Vec<ExpectedIssue> = issues.iter().map(ExpectedIssue::of).collect();
                let mut actual = actual;
                let mut expected = expected.clone();
                actual.sort();
                expected.sort();
                assert_eq!(
                    expected,
                    actual,
                    "issues for {} with set {set}",
                    input.display()
                );
            }
        }
    }
}

/// The canonical `model.json` and `model.yaml` first, then every alternative form of the same
/// model.
fn document_files(dir: &Path) -> Vec<PathBuf> {
    let mut files = vec![dir.join("model.json"), dir.join("model.yaml")];
    let mut others: Vec<PathBuf> = fs::read_dir(dir)
        .unwrap()
        .filter_map(|entry| entry.ok().map(|entry| entry.path()))
        .filter(|path| {
            matches!(
                path.extension().and_then(|e| e.to_str()),
                Some("json") | Some("yaml")
            ) && !path
                .file_name()
                .and_then(|name| name.to_str())
                .is_some_and(|name| name.starts_with("model."))
        })
        .collect();
    others.sort();
    files.extend(others);
    files
}

fn case_dirs(path: &str) -> Vec<PathBuf> {
    let mut dirs: Vec<PathBuf> = fs::read_dir(Path::new(CASES).join(path))
        .unwrap_or_else(|error| panic!("reading {path}: {error}"))
        .filter_map(|entry| entry.ok().map(|entry| entry.path()))
        .filter(|path| path.is_dir())
        .collect();
    dirs.sort();
    dirs
}

fn decode(file: &Path) -> GraphModel {
    let content = read(file);
    let decoded = if file.extension().and_then(|e| e.to_str()) == Some("yaml") {
        ontology_graph_spec::decode_yaml(&content)
    } else {
        ontology_graph_spec::decode_json(&content)
    };
    decoded.unwrap_or_else(|error| panic!("decoding {}: {error}", file.display()))
}

fn read(file: &Path) -> String {
    fs::read_to_string(file).unwrap_or_else(|error| panic!("reading {}: {error}", file.display()))
}

/// The issue comparison key: code, path and details, sorted so the comparison is order-free.
#[derive(Clone, Debug, PartialEq, Eq, PartialOrd, Ord)]
struct ExpectedIssue {
    code: String,
    path: Option<String>,
    details: BTreeMap<String, String>,
}

impl ExpectedIssue {
    fn of(issue: &Issue) -> ExpectedIssue {
        ExpectedIssue {
            code: issue.code.clone(),
            path: issue.path.clone(),
            details: issue.details.clone(),
        }
    }
}

fn expected_issues(file: &Path, case: &str) -> Vec<ExpectedIssue> {
    let raw = read(file);
    let issues: Vec<serde_json::Value> = serde_json::from_str(&raw)
        .unwrap_or_else(|error| panic!("parsing {}: {error}", file.display()));
    let mut parsed = Vec::new();
    for issue in issues {
        let mut map = BTreeMap::new();
        if let Some(details) = issue.get("details") {
            for (key, value) in details.as_object().into_iter().flatten() {
                let value = match value {
                    serde_json::Value::String(value) => value.clone(),
                    value => value.to_string(),
                };
                map.insert(key.clone(), value);
            }
        }
        parsed.push(ExpectedIssue {
            code: issue["code"].as_str().unwrap_or_default().to_owned(),
            path: issue["path"].as_str().map(str::to_owned),
            details: map,
        });
        assert!(
            !parsed.last().unwrap().code.is_empty(),
            "issue in {case} has a code"
        );
    }
    parsed
}

/// The 1-based line number of the first difference between two documents, for failure messages.
fn first_difference(expected: &str, actual: &str) -> usize {
    for (index, (expected, actual)) in expected.lines().zip(actual.lines()).enumerate() {
        if expected != actual {
            return index + 1;
        }
    }
    expected.lines().count().saturating_add(1)
}
