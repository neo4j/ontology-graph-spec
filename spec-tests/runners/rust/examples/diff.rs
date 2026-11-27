// Prints the decoded and re-encoded forms of a case document, for debugging differences.
// Run with: cargo run --example diff -- <case-file> [more-files...]

use ontology_graph_spec::GraphSpec;

fn main() {
    for path in std::env::args().skip(1) {
        let content = std::fs::read_to_string(&path).unwrap();
        let model = if path.ends_with(".yaml") {
            ontology_graph_spec::decode_yaml(&content)
        } else {
            ontology_graph_spec::decode_json(&content)
        }
        .unwrap_or_else(|error| panic!("decoding {path}: {error}"));
        let json = GraphSpec::Json.encode(&model).unwrap();
        let yaml = GraphSpec::Yaml.encode(&model).unwrap();
        println!("=== {path} (decoded, re-encoded JSON) ===\n{json}");
        println!("=== {path} (decoded, re-encoded YAML) ===\n{yaml}");
    }
}
