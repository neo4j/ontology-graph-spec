using System.Text.Json.Nodes;
using Neo4j.OntologyGraphSpec;
using Neo4j.OntologyGraphSpec.Validation;

namespace Neo4j.OntologyGraphSpec.SpecTests;

public sealed class SpecTests
{
    private static readonly string Cases = Path.Combine(AppContext.BaseDirectory, "cases");
    private static readonly IReadOnlyDictionary<string, ValidationSet> Sets = new Dictionary<string, ValidationSet>
    {
        ["core"] = ValidationSet.Core,
        ["labelled"] = ValidationSet.Labelled,
        ["consistent_schema"] = ValidationSet.ConsistentSchema,
        ["mappable"] = ValidationSet.Mappable,
        ["bulk_importable"] = ValidationSet.BulkImportable,
        ["well_constrained"] = ValidationSet.WellConstrained,
        ["all"] = ValidationSet.All
    };

    public static IEnumerable<object[]> Documents() => Directories("documents")
        .SelectMany(dir => Directory.GetFiles(dir).Where(file => Path.GetExtension(file) is ".json" or ".yaml"))
        .Order(StringComparer.Ordinal).Select(file => new object[] { Path.GetRelativePath(Cases, file) });

    public static IEnumerable<object[]> Validations() => Directories("validate")
        .SelectMany(Directory.GetDirectories).Order(StringComparer.Ordinal)
        .SelectMany(dir => new[] { "input.json", "input.yaml" }.Select(file => new object[] { Path.GetRelativePath(Cases, Path.Combine(dir, file)) }));

    public static IEnumerable<object[]> Migrations() => Directories("migrate")
        .Select(dir => new object[] { Path.GetRelativePath(Cases, dir) });

    [Fact]
    public void CorpusIsPresentAndEveryValidationSetIsCovered()
    {
        Assert.True(Documents().Count() >= 52, "The complete original document corpus must be present.");
        Assert.True(Validations().Count() >= 28, "Core and non-core validation inputs must be present.");
        Assert.True(Migrations().Count() >= 15, "Supported and unsupported migration routes must be present.");
        Assert.Equal(Sets.Keys.Order(StringComparer.Ordinal), Directories("validate").Select(Path.GetFileName).Order(StringComparer.Ordinal));
    }

    [Theory]
    [MemberData(nameof(Documents))]
    public void DocumentsProduceCanonicalOutput(string relativeFile)
    {
        var file = Path.Combine(Cases, relativeFile);
        var dir = Path.GetDirectoryName(file)!;
        var model = Codec(file).DecodeFromString(File.ReadAllText(file));
        EqualJson(File.ReadAllText(Path.Combine(dir, "model.json")), GraphSpec.Json.EncodeToString(model));
        Assert.Equal(File.ReadAllText(Path.Combine(dir, "model.yaml")), GraphSpec.Yaml.EncodeToString(model));
        Assert.Empty(model.Validate());
    }

    [Theory]
    [MemberData(nameof(Validations))]
    public void ValidationMatchesReferenceIssues(string relativeFile)
    {
        var file = Path.Combine(Cases, relativeFile);
        var dir = Path.GetDirectoryName(file)!;
        var set = Sets[Path.GetFileName(Path.GetDirectoryName(dir)!)];
        var issues = Codec(file).DecodeFromString(File.ReadAllText(file)).Validate(set);
        Assert.All(issues, issue => { Assert.False(string.IsNullOrWhiteSpace(issue.Message)); Assert.NotNull(issue.Path); });
        var actual = issues.Select(issue => IssueKey(issue.Code, issue.Path, issue.Details)).Order(StringComparer.Ordinal);
        var expected = JsonNode.Parse(File.ReadAllText(Path.Combine(dir, "issues.json")))!.AsArray()
            .Select(issue => IssueKey(issue!["code"]!.GetValue<string>(), issue["path"]?.GetValue<string>(),
                issue["details"]?.AsObject().ToDictionary(pair => pair.Key, pair => pair.Value!.GetValue<string>()) ?? []))
            .Order(StringComparer.Ordinal);
        Assert.Equal(expected, actual);
    }

    [Theory]
    [MemberData(nameof(Migrations))]
    public void MigrationMatchesReference(string relativeDirectory)
    {
        var dir = Path.Combine(Cases, relativeDirectory);
        var options = JsonNode.Parse(File.ReadAllText(Path.Combine(dir, "case.json")))!;
        string Migrate() => GraphSpec.Json.EncodeToString(
            GraphSpec.Json.DecodeFromString(File.ReadAllText(Path.Combine(dir, "input.json")), options["source_type"]!.GetValue<string>()),
            options["target_type"]!.GetValue<string>(), options["target_version"]!.GetValue<string>());
        if (options["fails"]!.GetValue<bool>())
        {
            var error = Assert.Throws<GraphSpecException>(Migrate);
            Assert.False(string.IsNullOrWhiteSpace(error.Message));
        }
        else EqualJson(File.ReadAllText(Path.Combine(dir, "expected.json")), Migrate());
    }

    private static GraphSpecCodec Codec(string file) => file.EndsWith(".yaml", StringComparison.Ordinal) ? GraphSpec.Yaml : GraphSpec.Json;

    private static string[] Directories(string folder)
    {
        var directories = Directory.GetDirectories(Path.Combine(Cases, folder)).Order(StringComparer.Ordinal).ToArray();
        if (directories.Length == 0) throw new InvalidOperationException($"Missing test cases: {folder}");
        return directories;
    }

    private static string IssueKey(string code, string? path, IReadOnlyDictionary<string, string> details) =>
        System.Text.Json.JsonSerializer.Serialize(new { code, path, details = details.OrderBy(pair => pair.Key, StringComparer.Ordinal).ToArray() });

    private static void EqualJson(string expected, string actual) =>
        Assert.True(JsonNode.DeepEquals(JsonNode.Parse(expected), JsonNode.Parse(actual)), $"Expected:\n{expected}\nActual:\n{actual}");
}
