module github.com/neo4j/ontology-graph-spec/spec-tests/runners/go

go 1.25.6

// go.work points at the local checkout; GOWORK=off uses this version.
require (
	github.com/neo4j/ontology-graph-spec/go v0.9.1
	github.com/stretchr/testify v1.10.0
)

require (
	github.com/davecgh/go-spew v1.1.1 // indirect
	github.com/ebitengine/purego v0.10.1 // indirect
	github.com/pmezard/go-difflib v1.0.0 // indirect
	gopkg.in/yaml.v3 v3.0.1 // indirect
)
