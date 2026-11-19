using System.Text.Json;
using System.Text.Json.Nodes;
using Neo4j.OntologyGraphSpec.Internal;
using Neo4j.OntologyGraphSpec.Model;

namespace Neo4j.OntologyGraphSpec.Tests;

public sealed class GeneratedModelTests
{
    [Fact]
    public void InvalidJsonElementGetsATransportDiagnostic()
    {
        var error = Assert.Throws<GraphSpecException>(() => ModelJson.Write(default(JsonElement), "encode"));
        Assert.Equal("encode", error.Operation);
        Assert.IsType<InvalidOperationException>(error.InnerException);
    }

    [Fact]
    public void RequiredNullsSurviveWhileAbsentOptionalMembersStayAbsent()
    {
        const string json = """{"anything":9007199254740993,"nullableItems":[null,"a"],"nullableList":null,"nullableMap":null,"nullableRef":null,"nullableValues":{"A":null},"typeArray":null}""";
        var model = JsonSerializer.Deserialize<Generated.nullable.Profile>(json)!;
        Assert.Equal(9007199254740993L, model.Anything.GetInt64());
        Assert.True(JsonNode.DeepEquals(JsonNode.Parse(json), JsonNode.Parse(JsonSerializer.Serialize(model))));
        Assert.Throws<JsonException>(() => JsonSerializer.Deserialize<Generated.nullable.Profile>("{}"));
    }

    [Fact]
    public void UnionVariantsCanBeSharedAndUsedDirectly()
    {
        const string json = """{"shapes":[{"type":"CIRCLE","radius":2},{"type":"POINT","x":1,"y":2}],"background":null,"origin":{"type":"POINT","x":3,"y":4},"marker":{"type":"POINT","x":5,"y":6},"expression":{"op":"add","left":{"op":"lit","value":1},"right":{"op":"lit","value":2}}}""";
        var model = JsonSerializer.Deserialize<Generated.unions.Drawing>(json)!;
        Assert.IsType<Generated.unions.Point>(model.Shapes[1]);
        Assert.IsType<Generated.unions.Point>(model.Marker);
        Assert.True(JsonNode.DeepEquals(JsonNode.Parse(json), JsonNode.Parse(JsonSerializer.Serialize(model))));
        Assert.Throws<JsonException>(() => JsonSerializer.Deserialize<Mapping>("""{"type":"UNKNOWN"}"""));
        Assert.Throws<JsonException>(() => JsonSerializer.Deserialize<IndexOption>("{}"));
    }

    [Fact]
    public void EnumsUseExactTokensAndRejectIntegers()
    {
        var type = JsonSerializer.Deserialize<Neo4jType>("\"LIST<STRING>\"");
        Assert.Equal("\"LIST<STRING>\"", JsonSerializer.Serialize(type).Replace("\\u003C", "<").Replace("\\u003E", ">"));
        Assert.Throws<JsonException>(() => JsonSerializer.Deserialize<Neo4jType>("123"));
        Assert.Throws<JsonException>(() => JsonSerializer.Serialize((Neo4jType)int.MaxValue));
    }

    [Fact]
    public void DottedKeysFalseAndZeroArePreserved()
    {
        IndexOption option = new VectorIndexOption { VectorDimensions = 0, VectorQuantizationEnabled = false };
        var json = JsonNode.Parse(JsonSerializer.Serialize(option))!;
        Assert.Equal(0L, json["vector.dimensions"]!.GetValue<long>());
        Assert.False(json["vector.quantization.enabled"]!.GetValue<bool>());
        Assert.Equal("VECTOR", json["type"]!.GetValue<string>());
    }

    [Fact]
    public void DictionaryKeysAreCaseSensitiveAndCustomDataOutlivesDeserialization()
    {
        const string json = """{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"test","nodes":{"Person":{},"person":{}},"extensions":{"custom":{"value":{"number":9007199254740993}}}}""";
        var model = ModelJson.Read<GraphModel>(json, "test");
        Assert.Equal(2, model.Nodes!.Count);
        Assert.Equal(9007199254740993L, model.Extensions!.Custom!["value"].GetProperty("number").GetInt64());
        Assert.Equal(GraphSpecSchema.Url, model.Schema);
    }
}
