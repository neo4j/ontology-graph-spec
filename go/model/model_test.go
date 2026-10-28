package model

import (
	"embed"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testdata embed.FS

func TestDataModelV3RoundTripJSON(t *testing.T) {

	raw, err := testdata.ReadFile("testdata/graph-spec-example.json")
	require.NoError(t, err)

	var graph GraphModel
	err = json.Unmarshal(raw, &graph)
	require.NoError(t, err)

	t.Log(fmt.Sprintf("Unmarshalled json into internal model: %v", graph))

	out, err := json.Marshal(graph)
	require.NoError(t, err)
	t.Log(fmt.Sprintf("Marshalled internal model back into json: %v", string(out)))
}

func TestGraphSpecAliasesRoundTripJSON(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/graph-spec-example.json")
	require.NoError(t, err)

	var graph GraphModel
	require.NoError(t, json.Unmarshal(raw, &graph))
	out, err := json.Marshal(graph)
	require.NoError(t, err)
	var back GraphModel
	require.NoError(t, json.Unmarshal(out, &back))

	actor := (*back.Nodes)["Actor"]
	acted := (*back.Relationships)["ACTED_IN"]
	require.Equal(t, []string{"Performer"}, *actor.Aliases)
	require.Equal(t, []string{"fullName", "label"}, *(*actor.Properties)["name"].Aliases)
	require.Equal(t, []string{"STARRED_IN"}, *acted.Aliases)
	require.Equal(t, []string{"characters"}, *(*acted.Properties)["roles"].Aliases)
}
