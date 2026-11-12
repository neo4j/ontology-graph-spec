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

	"github.com/neo4j/ontology-graph-spec/go/model"
	"github.com/neo4j/ontology-graph-spec/go/validation"
	"github.com/stretchr/testify/require"
)

const cases = "../../cases"

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
				input := filepath.Join(dir, "input.json")
				if !exists(input) {
					t.Skip("YAML input is not supported by the Go library")
				}

				issues, err := validation.Validate(decode(t, input), validation.Set(filepath.Base(set)))
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

func exists(file string) bool {
	_, err := os.Stat(file)
	return err == nil
}

func nonNil(details map[string]string) map[string]string {
	if details == nil {
		return map[string]string{}
	}
	return details
}
