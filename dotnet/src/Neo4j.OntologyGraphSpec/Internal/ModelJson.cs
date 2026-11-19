using System.Text.Json;

namespace Neo4j.OntologyGraphSpec.Internal;

// Internal transport only. Canonical documents and default omission are owned by Kotlin.
internal static class ModelJson
{
    internal static readonly JsonSerializerOptions Options = new()
    {
        // The core permits custom values nested 128 containers deep, inside the surrounding model.
        MaxDepth = 256,
        RespectNullableAnnotations = true
    };

    internal static T Read<T>(string json, string operation) where T : class
    {
        try
        {
            return JsonSerializer.Deserialize<T>(json, Options)
                ?? throw new JsonException("The native library returned a null result.");
        }
        catch (JsonException error)
        {
            throw new GraphSpecException(operation, "Invalid model transport: " + error.Message, error);
        }
    }

    internal static string Write<T>(T value, string operation)
    {
        try { return JsonSerializer.Serialize(value, Options); }
        catch (Exception error) when (error is JsonException or InvalidOperationException or NotSupportedException)
        {
            throw new GraphSpecException(operation, "Invalid model transport: " + error.Message, error);
        }
    }
}
