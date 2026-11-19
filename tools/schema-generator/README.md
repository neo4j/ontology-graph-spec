# Schema Generator

A tool that generates Go and C# types from the same JSON Schema parser and language-neutral model.

Not all JSON schema features are supported. This tool currently supports the subset that kotlinx-schema emits. Anything 
outside that subset is rejected with the JSON pointer of the offending schema.

## Usage

```shell  
go -C tools/schema-generator run . --schema "$PWD/ontology-graph-spec.schema.json" --out "$PWD/go/model/model.go" --package model
```

| Flag        | Description                                                           |  
|-------------|-----------------------------------------------------------------------|  
| `--schema`  | Path of the JSON Schema to read. Required.                            |  
| `--out`     | Path of the generated file to write. Required.                               |
| `--package` | Name of the Go package. Defaults to the name of the output directory. |  
  
| `--language` | `go` (default) or `csharp`. |
| `--namespace` | Required C# namespace when generating C#. |

For C#:

```shell
go -C tools/schema-generator run . --language csharp --schema ../../ontology-graph-spec.schema.json --out ../../dotnet/src/Neo4j.OntologyGraphSpec/Model/Generated/GraphModel.g.cs --namespace Neo4j.OntologyGraphSpec.Model
```

From the repository root, `pwsh ./dotnet/scripts/generate-models.ps1` first regenerates the schema from Kotlin and then
runs this command. Pass `-SkipSchema` only when working from an already current schema.

## C# output

The C# emitter generates sealed classes, typed string enums and discriminated union interfaces with JSON converters.
An interface allows one variant to participate in multiple unions without duplicating its properties. Optional values
are nullable and omitted when absent; required nullable values preserve null. Required fields use C# `required`.
Integers become `long`, numbers `double`, arrays `List<T>`, maps `Dictionary<string,T>` and unstructured values
`JsonElement`. JSON names and discriminator tokens remain unchanged; public identifiers use PascalCase. Name collisions
and invalid namespaces fail during generation. The root `$id` is emitted as `GraphSpecSchema.Url`.

Properties and definitions are deterministic. Existing Go output remains unchanged by the language option. Both emitters
share parser/model validation, and generator fixtures are compared against snapshots; the C# snapshots are also compiled
and exercised by the .NET tests. Runtime format validation remains in Kotlin, not in generated C# rules.

## Support and Functionality  
  
### Supported Keywords
Supported keywords are: `type`, `properties`, `required`, `additionalProperties`, `items`, `enum`, `const`, `oneOf`,  
`$ref`, `$defs`, `description` and `title`.

Notes:
* `description` becomes a doc comment
* The root's `title` names the root type
* `minimum` is ignored without error 
* `$schema`, `$id` and `$defs` are only allowed on the root - any other keyword is an error  
* The root and every entry of `$defs` must be one of:  
  - an object with `properties`, which becomes a struct  
  - a string `enum`, which becomes a named `string` type with a constant per value  
  - a `oneOf` of `$ref`s to object definitions, which becomes a union  
* Objects, enums and unions cannot be declared inline. Properties, list items and map values refer to them with a `$ref`  
of the form `#/$defs/Name`  
  
### Translations  
  
| Schema                                                           | Go                                   |  
|------------------------------------------------------------------|--------------------------------------|  
| `string`, `integer`, `number`, `boolean`                         | `string`, `int64`, `float64`, `bool` |  
| `{}`                                                             | `any`                                |  
| `array` with `items`                                             | `[]T`                                |  
| `object` with an `additionalProperties` schema                   | `map[string]T`                       |  
| `$ref`                                                           | the named type                       |  
  
Definitions and properties are sorted by name, so the output does not depend on the order of the schema.  
  
### Optional & Nullable Values  
  
A value is nullable when its schema is `"type": [T, "null"]`, or `"oneOf": [{"type": "null"}, {"$ref": ...}]` for a  
reference.  
  
| Property                  | Go field                                 |  
|---------------------------|------------------------------------------|  
| required and not nullable | `T`                                      |  
| required and nullable     | `*T`, encodes nil as `null`              |  
| not required              | `*T` with `omitempty`, nil leaves it out |  
  
Nullable list items and map values are pointers too (`[]*T`). Fields of type `any` are never pointers, since `any` can  
already hold nil.  
  
### Unions  
  
A `oneOf` of two or more `$ref`s to object definitions becomes a discriminated union. The discriminator is the one  
property of the first variant with a `const`, which is how kotlinx-schema marks the class discriminator. Every variant  
must declare it with a distinct `const`.  
  
```go  
type Shape struct {  
    ShapeVariant}  
  
type ShapeVariant interface {  
    ShapeType() string    isShape()}  
  
type ShapeCircle struct {  
    Radius float64 `json:"radius"`}  
