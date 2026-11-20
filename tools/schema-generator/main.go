// Command schema-generator generates Go or C# types from a JSON Schema.
//
// Usage:
//
//	schema-generator --schema spec.json --out model/model.go [--package model]
package main

import (
	"errors"
	"flag"
	"fmt"
	"go/token"
	"os"
	"path/filepath"

	"github.com/neo4j/ontology-graph-spec/tools/schema-generator/internal/generator"
)

func main() {
	if err := run(os.Args[1:]); err != nil {
		if !errors.Is(err, flag.ErrHelp) {
			fmt.Fprintln(os.Stderr, "schema-generator:", err)
		}
		os.Exit(2)
	}
}

func run(args []string) error {
	flags := flag.NewFlagSet("schema-generator", flag.ContinueOnError)
	schemaPath := flags.String("schema", "", "path of the JSON Schema to read (required)")
	out := flags.String("out", "", "path of the source file to write (required)")
	pkg := flags.String("package", "", "name of the Go package (default: the name of the output directory)")
	language := flags.String("language", "go", "output language: go or csharp")
	namespace := flags.String("namespace", "", "C# namespace (required for csharp)")
	if err := flags.Parse(args); err != nil {
		return err
	}
	if *schemaPath == "" || *out == "" || flags.NArg() > 0 {
		flags.Usage()
		return errors.New("--schema and --out are required, and no other arguments are accepted")
	}

	if *language != "csharp" && *pkg == "" {
		abs, err := filepath.Abs(*out)
		if err != nil {
			return err
		}
		*pkg = filepath.Base(filepath.Dir(abs))
		if !token.IsIdentifier(*pkg) {
			return fmt.Errorf("the output directory %q is not a valid package name, pass --package", *pkg)
		}
	}

	data, err := os.ReadFile(*schemaPath)
	if err != nil {
		return err
	}
	src, err := generator.Generate(data, generator.Options{
		Package:    *pkg,
		SchemaName: filepath.Base(*schemaPath),
		Language:   *language,
		Namespace:  *namespace,
	})
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(*out), 0o755); err != nil {
		return err
	}
	return os.WriteFile(*out, src, 0o644)
}
