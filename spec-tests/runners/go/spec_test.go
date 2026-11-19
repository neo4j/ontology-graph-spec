// Package spectests runs the Go library against the spec tests under spec-tests/cases at the repository root.
//
// The Go library works with JSON only, so YAML inputs and expected files are skipped.
package spectests

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sort"
	"testing"

	"github.com/neo4j/ontology-graph-spec/go/migration"
	"github.com/neo4j/ontology-graph-spec/go/model"
	"github.com/neo4j/ontology-graph-spec/go/validation"
	"github.com/stretchr/testify/require"
)

const cases = "../../cases"

func TestMigrate(t *testing.T) {
	for _, dir := range caseDirs(t, "migrate") {
		t.Run(filepath.Base(dir), func(t *testing.T) {
			var options struct {
				SourceType    string `json:"source_type"`
				TargetType    string `json:"target_type"`
				TargetVersion string `json:"target_version"`
				Fails         bool   `json:"fails"`
			}
			require.NoError(t, json.Unmarshal([]byte(read(t, filepath.Join(dir, "case.json"))), &options))
			graph, err := migration.ToGraphSpec(read(t, filepath.Join(dir, "input.json")), migration.ModelType(options.SourceType))
			var actual string
			if err == nil {
				actual, err = migration.FromGraphSpec(graph, migration.ModelType(options.TargetType), migration.ModelVersion(options.TargetVersion))
			}
			if options.Fails {
				require.Error(t, err)
				require.NotEmpty(t, err.Error())
			} else {
				require.NoError(t, err)
				require.Equal(t,
					migrationJSON(t, read(t, filepath.Join(dir, "expected.json")), options.TargetType),
					migrationJSON(t, actual, options.TargetType))
			}
		})
	}
}

func migrationJSON(t *testing.T, content, targetType string) map[string]any {
	t.Helper()
	var document map[string]any
	require.NoError(t, json.Unmarshal([]byte(content), &document))
	if targetType != "data_model" && targetType != "data_model_wrapped" {
		return document
	}

	path := []string{"graphMappingRepresentation", "dataSourceSchema"}
	if targetType == "data_model_wrapped" {
		path = append([]string{"dataModel"}, path...)
	}
	current := document
	for _, key := range path {
		require.IsType(t, map[string]any{}, current[key])
		current = current[key].(map[string]any)
	}
	require.IsType(t, []any{}, current["tableSchemas"])
	tables := current["tableSchemas"].([]any)
	// Table schemas originate in a graph-spec object keyed by table name. Go's JSON
	// encoder sorts those keys; their insertion order is not part of that contract.
	// Preserve every table, field and all other array ordering in the comparison.
	sort.SliceStable(tables, func(i, j int) bool {
		return tables[i].(map[string]any)["name"].(string) < tables[j].(map[string]any)["name"].(string)
	})
	return document
}

func TestDocuments(t *testing.T) {
	for _, dir := range caseDirs(t, "documents") {
		t.Run(filepath.Base(dir), func(t *testing.T) {
			// Note: the Go library currently decodes a document as written, so each form must
			// round-trip to itself and validate clean, rather than encode to the canonical model.json
			files, err := filepath.Glob(filepath.Join(dir, "*.json"))
			require.NoError(t, err)
			for _, file := range files {
				graph := decode(t, file)

				encoded, err := json.Marshal(graph)
				require.NoError(t, err)
				require.JSONEq(t, read(t, file), string(encoded), "%s round trip", filepath.Base(file))

				issues, err := validation.Validate(graph, validation.SetCore)
				require.NoError(t, err)
				require.Empty(t, issues, "%s core issues", filepath.Base(file))
			}
		})
	}
}

func TestValidate(t *testing.T) {
	type validationIssue struct {
		Code    string            `json:"code"`
		Path    *string           `json:"path"`
		Details map[string]string `json:"details"`
	}

	for _, set := range caseDirs(t, "validate") {
		for _, dir := range caseDirs(t, filepath.Join("validate", filepath.Base(set))) {
			name := filepath.Base(set) + "/" + filepath.Base(dir)
			t.Run(name, func(t *testing.T) {
				// The Go library has no YAML decoder, so only input.json is validated.
				issues, err := validation.Validate(decode(t, filepath.Join(dir, "input.json")), validation.Set(filepath.Base(set)))
				require.NoError(t, err)

				actual := make([]validationIssue, 0, len(issues))
				for _, issue := range issues {
					require.NotEmpty(t, issue.Message, "issue %s has a message", issue.Code)
					require.NotEmpty(t, issue.Path, "issue %s has a path", issue.Code)
					path := issue.Path
					actual = append(actual, validationIssue{Code: issue.Code, Path: &path, Details: nonNil(issue.Details)})
				}

				var expected []validationIssue
				require.NoError(t, json.Unmarshal([]byte(read(t, filepath.Join(dir, "issues.json"))), &expected))
				for i := range expected {
					expected[i].Details = nonNil(expected[i].Details)
				}

				require.ElementsMatch(t, expected, actual, "issues for set %s", filepath.Base(set))
			})
		}
	}
}

func caseDirs(t *testing.T, path string) []string {
	t.Helper()
	entries, err := os.ReadDir(filepath.Join(cases, path))
	require.NoError(t, err)
	var dirs []string
	for _, entry := range entries {
		if entry.IsDir() {
			dirs = append(dirs, filepath.Join(cases, path, entry.Name()))
		}
	}
	sort.Strings(dirs)
	return dirs
}

func decode(t *testing.T, file string) model.GraphModel {
	t.Helper()
	var graph model.GraphModel
	require.NoError(t, json.Unmarshal([]byte(read(t, file)), &graph))
	return graph
}

func read(t *testing.T, file string) string {
	t.Helper()
	raw, err := os.ReadFile(file)
	require.NoError(t, err)
	return string(raw)
}

func nonNil(details map[string]string) map[string]string {
	if details == nil {
		return map[string]string{}
	}
	return details
}
