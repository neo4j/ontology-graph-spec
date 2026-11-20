using System.Text.Json.Nodes;
using Neo4j.OntologyGraphSpec.Model;
using Neo4j.OntologyGraphSpec.Validation;

namespace Neo4j.OntologyGraphSpec.Tests;

public sealed class GraphSpecTests
{
    private static string Input(string properties = "") =>
        "{\"schema\":\"" + GraphSpecSchema.Url + "\",\"version\":\"1\",\"id\":\"test\"" + properties + "}";

    [Fact]
    public void SharedCodeExpandsShorthandAndCarriesEffectiveDefaults()
    {
        var model = GraphSpec.Json.DecodeFromString(Input(",\"nodes\":{\"person\":{\"label\":\"Person\",\"properties\":{\"id\":{\"type\":\"STRING\",\"key\":true}}}}"));
        var node = model.Nodes!["person"];
        Assert.Null(node.Label);
        Assert.Equal("Person", node.Labels!.Identifier);
        Assert.Single(node.Constraints!);
        Assert.Equal("", node.Description);
        Assert.Empty(model.Validate());
        Assert.Equal("1.1.0", GraphSpec.LatestVersion);
    }

    [Fact]
    public void BuiltInSetsComposeWithoutDuplicates()
    {
        var model = GraphSpec.Json.DecodeFromString(Input(",\"nodes\":{\"person\":{\"label\":\"Person:Detail\"}}"));
        Assert.Contains(model.Validate(ValidationSet.BulkImportable), issue => issue.Code == "invalid_node_label_token");
        Assert.Equal(model.Validate(ValidationSet.All).Select(i => i.Code),
            model.Validate(ValidationSet.All, ValidationSet.Core, ValidationSet.Labelled, ValidationSet.All).Select(i => i.Code));
        Assert.Throws<ArgumentOutOfRangeException>(() => model.Validate((ValidationSet)99));
    }

    [Fact]
    public void DirectEncodingAndValidationDoNotMutateCallerModels()
    {
        var model = new GraphModel
        {
            Schema = GraphSpecSchema.Url, Version = "1", Id = "created",
            Nodes = new() { ["person"] = new Node { Label = "Person" } }
        };
        var json = GraphSpec.Json.EncodeToString(model);
        Assert.Equal("Person", JsonNode.Parse(json)!["nodes"]!["person"]!["label"]!.GetValue<string>());
        Assert.Empty(model.Validate());
        Assert.Equal("Person", model.Nodes["person"].Label);
        Assert.Null(model.Nodes["person"].Labels);
    }

    [Fact]
    public void UnknownFieldsUseReferenceLeniency()
    {
        var model = GraphSpec.Json.DecodeFromString(Input(",\"future_field\":123"));
        Assert.DoesNotContain("future_field", GraphSpec.Json.EncodeToString(model));
    }

    [Theory]
    [InlineData("not JSON")]
    [InlineData("{}")]
    public void MalformedInputsReturnCodecErrors(string input) => Assert.Throws<GraphSpecException>(() => GraphSpec.Json.DecodeFromString(input));

    [Fact]
    public void CustomNullAndDepthAreEnforcedByTheCore()
    {
        Assert.Throws<GraphSpecException>(() => GraphSpec.Json.DecodeFromString(Input(",\"extensions\":{\"custom\":{\"bad\":[null]}}")));
        var nested = "0";
        for (var i = 0; i < 100; i++) nested = "[" + nested + "]";
        var model = GraphSpec.Json.DecodeFromString(Input(",\"extensions\":{\"custom\":{\"deep\":" + nested + "}}"));
        Assert.NotEmpty(GraphSpec.Json.EncodeToString(model));
        for (var i = 0; i < 40; i++) nested = "[" + nested + "]";
        Assert.Throws<GraphSpecException>(() => GraphSpec.Json.DecodeFromString(Input(",\"extensions\":{\"custom\":{\"deep\":" + nested + "}}")));
    }

    [Fact]
    public void NativeIntRangeIsEnforced()
    {
        var model = GraphSpec.Json.DecodeFromString(Input(",\"relationships\":{\"r\":{\"type\":\"R\",\"from\":{},\"to\":{}}}"));
        model.Relationships!["r"].From.Count = long.MaxValue;
        Assert.Throws<GraphSpecException>(() => GraphSpec.Json.EncodeToString(model));
    }

    [Fact]
    public void UnicodeEscapedNullAndLargeResponsesSurviveTheNativeBoundary()
    {
        var description = "café 🌍 " + new string('x', 20_000) + "\0";
        var input = JsonNode.Parse(Input())!;
        input["description"] = description;
        var model = GraphSpec.Json.DecodeFromString(input.ToJsonString());
        Assert.Equal(description, model.Description);
        Assert.Equal(description, GraphSpec.Json.DecodeFromString(GraphSpec.Json.EncodeToString(model)).Description);
        Assert.Throws<ArgumentException>(() => GraphSpec.Json.DecodeFromString("{}\0"));
    }

    [Fact]
    public async Task IndependentModelsCanBeProcessedConcurrently()
    {
        await Task.WhenAll(Enumerable.Range(0, 32).Select(_ => Task.Run(() =>
        {
            var model = GraphSpec.Json.DecodeFromString(Input());
            Assert.Empty(model.Validate());
            Assert.NotEmpty(GraphSpec.Yaml.EncodeToString(model));
        }, TestContext.Current.CancellationToken)));
    }
}
