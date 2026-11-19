using Neo4j.OntologyGraphSpec.Model;
using Neo4j.OntologyGraphSpec.Validation;

namespace Neo4j.OntologyGraphSpec.Internal;

internal sealed class NativeGraphSpecEngine : IGraphSpecEngine
{
    internal static readonly NativeGraphSpecEngine Instance = new();

    public GraphModel Decode(string content, string format, string sourceType)
    {
        var json = NativeCall.Invoke("decode", output => NativeMethods.Decode(content, format, sourceType, output, output.Length));
        return ModelJson.Read<GraphModel>(json, "decode");
    }

    public string Encode(GraphModel model, string format, string targetType, string targetVersion)
    {
        var json = ModelJson.Write(model, "encode");
        return NativeCall.Invoke("encode", output => NativeMethods.Encode(json, format, targetType, targetVersion, output, output.Length));
    }

    public IReadOnlyList<Issue> Validate(GraphModel model, IReadOnlyList<ValidationSet> sets)
    {
        var labels = sets.Count == 0 ? "core" : string.Join(",", sets.Select(Label));
        var json = ModelJson.Write(model, "validate");
        var result = NativeCall.Invoke("validate", output => NativeMethods.Validate(json, labels, output, output.Length));
        return ModelJson.Read<List<Issue>>(result, "validate").AsReadOnly();
    }

    private static string Label(ValidationSet set) => set switch
    {
        ValidationSet.Core => "core",
        ValidationSet.Labelled => "labelled",
        ValidationSet.ConsistentSchema => "consistent_schema",
        ValidationSet.Mappable => "mappable",
        ValidationSet.BulkImportable => "bulk_importable",
        ValidationSet.WellConstrained => "well_constrained",
        ValidationSet.All => "all",
        _ => throw new ArgumentOutOfRangeException(nameof(set), set, "Unknown validation set.")
    };
}
