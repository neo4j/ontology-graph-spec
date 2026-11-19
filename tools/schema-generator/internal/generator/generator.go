// Package generator turns a JSON Schema into Go types.
package generator

import (
	"fmt"
	"go/token"

	"github.com/neo4j/ontology-graph-spec/tools/schema-generator/internal/csharp"
	"github.com/neo4j/ontology-graph-spec/tools/schema-generator/internal/golang"
	"github.com/neo4j/ontology-graph-spec/tools/schema-generator/internal/model"
	"github.com/neo4j/ontology-graph-spec/tools/schema-generator/internal/schema"
)

type Options struct {
	Package    string
	SchemaName string
	Language   string
	Namespace  string
}

func Generate(schemaJSON []byte, opts Options) ([]byte, error) {
	if opts.Language != "" && opts.Language != "go" && opts.Language != "csharp" {
		return nil, fmt.Errorf("unsupported language %q", opts.Language)
	}
	if opts.Language != "csharp" && !token.IsIdentifier(opts.Package) {
		return nil, fmt.Errorf("%q is not a valid Go package name", opts.Package)
	}
	doc, err := schema.Load(schemaJSON)
	if err != nil {
		return nil, err
	}
	m, err := model.Build(doc)
	if err != nil {
		return nil, err
	}
	if opts.Language == "csharp" {
		return csharp.Generate(m, csharp.Options{Namespace: opts.Namespace, SchemaName: opts.SchemaName, SchemaID: doc.ID})
	}
	return golang.Generate(m, golang.Options{Package: opts.Package, SchemaName: opts.SchemaName})
}
