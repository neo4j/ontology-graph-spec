package validation_test

import (
	"embed"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/neo4j/ontology-graph-spec/go/model"
	"github.com/neo4j/ontology-graph-spec/go/validation"
	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testdata embed.FS

func TestValidate(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/invalid-graph-model.json")
	require.NoError(t, err)

	var graph model.GraphModel
	err = json.Unmarshal(raw, &graph)
	require.NoError(t, err)

	res, err := validation.Validate(graph, validation.SetAll)
	require.NoError(t, err)

	t.Log(fmt.Sprintf("Validated graph: %v", res))
	require.Len(t, res, 5)

	codes := make([]string, len(res))
	for i := range res {
		codes[i] = res[i].Code
	}
	require.ElementsMatch(t, []string{
		"invalid_node_type_constraint_property_count",
		"invalid_node_exist_constraint_property_count",
		"missing_node_properties",
		"missing_node_constraint_properties",
		"missing_node_constraint_properties",
	}, codes)
}

func TestValidate_Sets(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/invalid-graph-model.json")
	require.NoError(t, err)

	var graph model.GraphModel
	err = json.Unmarshal(raw, &graph)
	require.NoError(t, err)

	validations := []validation.Set{
		validation.SetCore,
		validation.SetLabelled,
		validation.SetConsistentSchema,
		validation.SetMappable,
		validation.SetBulkImportable,
		validation.SetWellConstrained,
		validation.SetAll,
	}

	for _, v := range validations {
		t.Run(fmt.Sprintf("validation runs for set %s", v), func(t *testing.T) {
			_, err := validation.Validate(graph, v)
			require.NoError(t, err)
		})
	}
}

func TestValidate_MultipleSets(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/invalid-graph-model.json")
	require.NoError(t, err)

	var graph model.GraphModel
	require.NoError(t, json.Unmarshal(raw, &graph))

	all, err := validation.Validate(graph, validation.SetAll)
	require.NoError(t, err)

	t.Run("overlapping sets report issues once", func(t *testing.T) {
		res, err := validation.Validate(graph, validation.SetAll, validation.SetCore, validation.SetLabelled)
		require.NoError(t, err)
		require.ElementsMatch(t, all, res)
	})

	t.Run("every set combined matches all", func(t *testing.T) {
		res, err := validation.Validate(graph,
			validation.SetCore,
			validation.SetLabelled,
			validation.SetConsistentSchema,
			validation.SetMappable,
			validation.SetBulkImportable,
			validation.SetWellConstrained,
		)
		require.NoError(t, err)
		require.ElementsMatch(t, all, res)
	})

	t.Run("a subset of sets reports fewer issues", func(t *testing.T) {
		res, err := validation.Validate(graph, validation.SetLabelled, validation.SetWellConstrained)
		require.NoError(t, err)
		require.NotEmpty(t, res)
		require.Less(t, len(res), len(all))
	})

	t.Run("no set given uses core default", func(t *testing.T) {
		core, err := validation.Validate(graph, validation.SetCore)
		require.NoError(t, err)

		res, err := validation.Validate(graph)
		require.NoError(t, err)

		require.ElementsMatch(t, core, res)
	})

	t.Run("unknown set is an error", func(t *testing.T) {
		_, err := validation.Validate(graph, validation.SetCore, "unknown")
		require.ErrorContains(t, err, "unknown validation type: unknown")
	})
}
