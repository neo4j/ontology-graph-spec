package migration_test

import (
	"embed"
	"encoding/json"
	"fmt"
	"os"
	"testing"

	"github.com/neo4j/ontology-graph-spec/go/migration"
	"github.com/neo4j/ontology-graph-spec/go/model"
	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testdata embed.FS

func TestV3ToGraphSpecMigration(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/northwind.json")
	require.NoError(t, err)

	result, err := migration.ToGraphSpec(string(raw), migration.ModelTypeDataModel)
	require.NoError(t, err)
	require.NotEmpty(t, result.Extensions.DataSourceMappings)

	resultBytes, err := json.Marshal(result)
	require.NoError(t, err)
	t.Log(fmt.Sprintf("Transformed graph: %v", string(resultBytes)))

	// Check round trip back to V3 data model.
	back, err := migration.FromGraphSpec(result, migration.ModelTypeDataModel, migration.ModelVersionDataModelV30)
	require.NoError(t, err)
	require.NotEmpty(t, back)
}

func TestGraphSpecToV3Migration(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/graph-spec-example.json")
	require.NoError(t, err)

	var graph model.GraphModel
	err = json.Unmarshal(raw, &graph)
	require.NoError(t, err)

	res, err := migration.FromGraphSpec(graph, migration.ModelTypeDataModel, migration.ModelVersionDataModelV30)
	require.NoError(t, err)
	require.NotNil(t, res)
	t.Log(fmt.Sprintf("Transformed graph: %v", res))
}

func TestGraphSpecLatestMatchesSchemaID(t *testing.T) {
	raw, err := os.ReadFile("../../ontology-graph-spec.schema.json")
	require.NoError(t, err)

	var schema struct {
		ID string `json:"$id"`
	}
	require.NoError(t, json.Unmarshal(raw, &schema))

	require.Equal(t,
		fmt.Sprintf("https://neo4j.com/docs/ontology-graph-spec/%s/schema.json", migration.ModelVersionGraphSpecLatest),
		schema.ID,
		"ModelVersionGraphSpecLatest must match the spec version in the schema $id",
	)
}
