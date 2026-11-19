namespace Neo4j.OntologyGraphSpec;

/// <summary>A codec, migration, transport, or native-library failure. Semantic validation issues are returned separately.</summary>
public sealed class GraphSpecException : Exception
{
    public string Operation { get; }

    internal GraphSpecException(string operation, string message, Exception? innerException = null)
        : base($"GraphSpec {operation}: {message}", innerException) => Operation = operation;
}
