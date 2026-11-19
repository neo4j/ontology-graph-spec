using System.Text.Json.Serialization;

namespace Neo4j.OntologyGraphSpec.Validation;

/// <summary>A semantic validation issue from the shared core.</summary>
public sealed record Issue
{
    [JsonPropertyName("code")]
    public required string Code { get; init; }

    [JsonPropertyName("message")]
    public required string Message { get; init; }

    [JsonPropertyName("path")]
    public string? Path { get; init; }

    [JsonPropertyName("details")]
    public IReadOnlyDictionary<string, string> Details { get; init; } = new Dictionary<string, string>();
}
