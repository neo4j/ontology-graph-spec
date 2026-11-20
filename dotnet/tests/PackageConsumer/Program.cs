using System.Reflection;
using System.Runtime.InteropServices;
using Neo4j.OntologyGraphSpec;
using Neo4j.OntologyGraphSpec.Model;
using Neo4j.OntologyGraphSpec.Validation;

if (args.Length != 2 || args[0] != RuntimeInformation.RuntimeIdentifier)
    throw new InvalidOperationException($"Unexpected test host: {RuntimeInformation.RuntimeIdentifier}");

var assembly = typeof(GraphSpec).Assembly;
var packageVersion = args[1].Split('+')[0];
var numericVersion = Version.Parse(packageVersion.Split('-')[0]);
var expectedAssemblyVersion = new Version(numericVersion.Major, numericVersion.Minor, numericVersion.Build, 0);
Require(assembly.GetName().Version == expectedAssemblyVersion, $"Assembly version must match package {packageVersion}");
Require(assembly.GetCustomAttribute<AssemblyFileVersionAttribute>()?.Version == expectedAssemblyVersion.ToString(),
    $"File version must match package {packageVersion}");
Require(assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion.Split('+')[0] == packageVersion,
    $"Informational version must match package {packageVersion}");

var json = "{\"schema\":\"" + GraphSpecSchema.Url + "\",\"version\":\"1\",\"id\":\"package-test\","
    + "\"nodes\":{\"person\":{\"label\":\"Person\",\"properties\":{\"id\":{\"type\":\"STRING\",\"key\":true}}}}}";
var model = GraphSpec.Json.DecodeFromString(json);
Require(model.Nodes!["person"].Labels!.Identifier == "Person", "Shorthand decoding");
Require(model.Validate().Count == 0, "Core validation");
var yaml = GraphSpec.Yaml.EncodeToString(model);
Require(GraphSpec.Json.EncodeToString(GraphSpec.Yaml.DecodeFromString(yaml)) == GraphSpec.Json.EncodeToString(model), "YAML round trip");

foreach (var target in new[] { ModelType.DataModel, ModelType.DataModelWrapped })
{
    var exported = GraphSpec.Json.EncodeToString(model, target, "3.0.0");
    var imported = GraphSpec.Json.DecodeFromString(exported, target);
    Require(imported.Nodes!.Values.Any(n => n.Labels?.Identifier == "Person"), "JSON migration");
    var exportedYaml = GraphSpec.Yaml.EncodeToString(model, target, "3.0.0");
    Require(GraphSpec.Yaml.DecodeFromString(exportedYaml, target).Nodes!.Count == 1, "YAML migration");
}
var upgraded = GraphSpec.Json.DecodeFromString(json.Replace("/1.1.0/", "/1.0.0/", StringComparison.Ordinal));
Require(upgraded.Schema == GraphSpecSchema.Url, "Graph spec upgrade");
var invalid = new GraphModel { Schema = GraphSpecSchema.Url, Version = "1", Id = "invalid", Nodes = new() { ["n"] = new Node { Label = "Invalid:Token" } } };
Require(invalid.Validate(ValidationSet.BulkImportable).Any(i => i.Code == "invalid_node_label_token"), "Bulk import validation");
try { GraphSpec.Json.DecodeFromString("{}"); throw new InvalidOperationException("Malformed input was accepted"); }
catch (GraphSpecException error) { Require(!string.IsNullOrWhiteSpace(error.Message), "Error diagnostic"); }
Parallel.For(0, 100, _ => Require(GraphSpec.Json.DecodeFromString(json).Validate().Count == 0, "Concurrent calls"));
Console.WriteLine($"PASS: NuGet {packageVersion} metadata, decode/encode JSON/YAML, validation, migration, errors and concurrency on {args[0]}");

static void Require(bool condition, string operation)
{
    if (!condition) throw new InvalidOperationException(operation + " failed");
}
