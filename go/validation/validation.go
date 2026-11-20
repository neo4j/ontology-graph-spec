package validation

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/neo4j/ontology-graph-spec/go/internal/bridge"
	"github.com/neo4j/ontology-graph-spec/go/model"
)

type Issue struct {
	Code    string            `json:"code"`
	Message string            `json:"message"`
	Path    string            `json:"path"`
	Details map[string]string `json:"details"`
}

type Set string

const (
	SetCore             Set = "core"
	SetLabelled         Set = "labelled"
	SetConsistentSchema Set = "consistent_schema"
	SetMappable         Set = "mappable"
	SetBulkImportable   Set = "bulk_importable"
	SetWellConstrained  Set = "well_constrained"
	SetAll              Set = "all"
)

// Validate checks the model against the union of the given sets.
// If no set is provided the default [SetCore] is used.
func Validate(model model.GraphModel, sets ...Set) ([]Issue, error) {
	validationSets := string(SetCore)
	if len(sets) != 0 {
		labels := make([]string, len(sets))
		for i, set := range sets {
			labels[i] = string(set)
		}
		validationSets = strings.Join(labels, ",")
	}

	bytes, err := json.Marshal(model)
	if err != nil {
		return nil, err
	}
	res, err := bridge.Call(bridge.Validate, bytes, validationSets)
	if err != nil {
		return nil, err
	}
	var issues []Issue
	if err := json.Unmarshal(res, &issues); err != nil {
		return nil, fmt.Errorf("error parsing validation issues: %w", err)
	}
	return issues, nil
}
