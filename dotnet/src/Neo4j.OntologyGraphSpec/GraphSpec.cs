using Neo4j.OntologyGraphSpec.Internal;
using Neo4j.OntologyGraphSpec.Model;

namespace Neo4j.OntologyGraphSpec;

/// <summary>Read and write ontology documents using the shared Kotlin implementation.</summary>
public static class GraphSpec
{
    public static GraphSpecCodec Json { get; } = new("json", NativeGraphSpecEngine.Instance);
    public static GraphSpecCodec Yaml { get; } = new("yaml", NativeGraphSpecEngine.Instance);

    /// <summary>The specification version generated from the schema; independent of package and model versions.</summary>
    public static string LatestVersion { get; } = new Uri(GraphSpecSchema.Url).Segments[^2].TrimEnd('/');
}

/// <summary>Document families supported by the built-in migration paths.</summary>
public static class ModelType
{
    public const string GraphSpec = "graph_spec";
    public const string DataModel = "data_model";
    public const string DataModelWrapped = "data_model_wrapped";
}
