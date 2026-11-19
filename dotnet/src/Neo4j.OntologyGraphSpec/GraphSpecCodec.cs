using Neo4j.OntologyGraphSpec.Internal;
using Neo4j.OntologyGraphSpec.Model;

namespace Neo4j.OntologyGraphSpec;

/// <summary>A reusable JSON or YAML codec. Operations do not mutate the supplied C# model.</summary>
public sealed class GraphSpecCodec
{
    private readonly string format;
    private readonly IGraphSpecEngine engine;

    internal GraphSpecCodec(string format, IGraphSpecEngine engine)
    {
        this.format = format;
        this.engine = engine;
    }

    /// <summary>Decode, migrate to the latest graph specification, and expand shorthand.</summary>
    public GraphModel DecodeFromString(string content, string sourceType = ModelType.GraphSpec)
    {
        NativeCall.CheckInput(content, nameof(content));
        NativeCall.CheckInput(sourceType, nameof(sourceType));
        return engine.Decode(content, format, sourceType);
    }

    /// <summary>Encode a model, optionally migrating it to a supported target type/version.</summary>
    public string EncodeToString(GraphModel model, string targetType = ModelType.GraphSpec, string? targetVersion = null)
    {
        ArgumentNullException.ThrowIfNull(model);
        NativeCall.CheckInput(targetType, nameof(targetType));
        targetVersion ??= GraphSpec.LatestVersion;
        NativeCall.CheckInput(targetVersion, nameof(targetVersion));
        return engine.Encode(model, format, targetType, targetVersion);
    }
}
