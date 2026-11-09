// Runs the npm package against the spec tests under spec-tests/cases.
//
// The package under test is whatever `@neo4j/ontology-graph-spec` resolves to: the local build by default, or a
// published version after `npm install @neo4j/ontology-graph-spec@<version> --no-save`.
import { test } from "node:test";
import assert from "node:assert/strict";
import { readdirSync, readFileSync } from "node:fs";
import { basename, join } from "node:path";
import { GraphModel, GraphSpec, Validations } from "@neo4j/ontology-graph-spec";

const cases = join(import.meta.dirname, "../../cases");

const sets = {
  core: Validations.core,
  labelled: Validations.labelled,
  consistent_schema: Validations.consistentSchema,
  mappable: Validations.mappable,
  bulk_importable: Validations.bulkImportable,
  well_constrained: Validations.wellConstrained,
  all: Validations.all,
};

const caseDirs = (path) =>
  readdirSync(join(cases, path), { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => join(cases, path, entry.name))
    .sort();

const read = (file) => readFileSync(file, "utf8");
const inputFile = (dir) => readdirSync(dir).filter((name) => name.startsWith("input.")).map((name) => join(dir, name))[0];
// The canonical model.json and model.yaml first, then every alternative form of the same model.
const documentFiles = (dir) => [
  join(dir, "model.json"),
  join(dir, "model.yaml"),
  ...readdirSync(dir).filter((name) => /\.(json|yaml)$/.test(name) && !name.startsWith("model.")).sort().map((name) => join(dir, name)),
];
const decode = (file) => (file.endsWith(".yaml") ? GraphSpec.Yaml : GraphSpec.Json).decodeFromString(read(file));
const validate = (model, set) => GraphModel.validate(model, sets[set]).asJsReadonlyArrayView();

// The part of an issue the spec tests compare: everything but the message.
const golden = (issue) => ({
  code: issue.code,
  path: issue.path ?? null,
  details: Object.fromEntries(issue.details.asJsReadonlyMapView()),
});
const sorted = (issues) => [...issues].sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)));

for (const dir of caseDirs("documents")) {
  test(`documents/${basename(dir)}`, () => {
    const json = join(dir, "model.json");
    const yaml = join(dir, "model.yaml");
    for (const file of documentFiles(dir)) {
      const model = decode(file);
      assert.deepEqual(JSON.parse(GraphSpec.Json.encodeToString(model)), JSON.parse(read(json)), `${basename(file)} encoded as JSON`);
      assert.equal(GraphSpec.Yaml.encodeToString(model), read(yaml), `${basename(file)} encoded as YAML`);
    }
    assert.deepEqual([...validate(decode(json), "core")], [], "core issues");
  });
}

for (const set of caseDirs("validate")) {
  for (const code of caseDirs(`validate/${basename(set)}`)) {
    for (const dir of caseDirs(`validate/${basename(set)}/${basename(code)}`)) {
      test(`validate/${basename(set)}/${basename(code)}/${basename(dir)}`, () => {
        const issues = validate(decode(inputFile(dir)), basename(set));
        for (const issue of issues) {
          assert.ok(issue.message.trim(), `issue ${issue.code} has a message`);
          assert.ok(issue.path != null, `issue ${issue.code} has a path`);
        }
        const expected = JSON.parse(read(join(dir, "issues.json")));
        assert.deepEqual(sorted(issues.map(golden)), sorted(expected), `issues for set ${basename(set)}`);
      });
    }
  }
}
