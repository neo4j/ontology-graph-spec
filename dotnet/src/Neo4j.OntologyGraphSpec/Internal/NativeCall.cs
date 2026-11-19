using System.Runtime.InteropServices;
using System.Text;

namespace Neo4j.OntologyGraphSpec.Internal;

internal static class NativeCall
{
    private static readonly UTF8Encoding Utf8 = new(false, true);

    internal static void CheckInput(string input, string parameter)
    {
        ArgumentNullException.ThrowIfNull(input, parameter);
        if (input.Contains('\0'))
            throw new ArgumentException("Native input strings cannot contain a literal NUL character.", parameter);
    }

    internal static string Invoke(string operation, Func<byte[], int> call)
    {
        EnsureSupportedPlatform();
        try
        {
            var buffer = new byte[4096];
            for (var attempt = 0; attempt < 2; attempt++)
            {
                var result = call(buffer);
                if (result == -1) throw new GraphSpecException(operation, "The native library rejected the call arguments.");
                if (result == int.MinValue)
                    throw new GraphSpecException(operation, "The native library could not allocate a response. Check model size and GRAPHSPEC_MAX_HEAP_BYTES.");
                if (result < -1)
                {
                    var required = -result;
                    if (attempt != 0 || required <= buffer.Length || required > Array.MaxLength)
                        throw new GraphSpecException(operation, $"Invalid native buffer resize request: {required}.");
                    buffer = new byte[required];
                    continue;
                }
                if (result < 1 || result > buffer.Length)
                    throw new GraphSpecException(operation, $"Invalid native response length: {result}.");
                var payload = Utf8.GetString(buffer, 1, result - 1);
                return buffer[0] switch
                {
                    0 => payload,
                    1 => throw new GraphSpecException(operation, payload),
                    _ => throw new GraphSpecException(operation, $"Unknown native status: {buffer[0]}.")
                };
            }
            throw new GraphSpecException(operation, "Native response exceeded the retry limit.");
        }
        catch (Exception error) when (error is DllNotFoundException or EntryPointNotFoundException or BadImageFormatException)
        {
            throw new GraphSpecException(operation,
                $"Could not load a compatible graphdatamodel native library for {RuntimeInformation.RuntimeIdentifier}. Restore the matching package runtime assets. {error.Message}", error);
        }
        catch (DecoderFallbackException error)
        {
            throw new GraphSpecException(operation, "Native response is not valid UTF-8.", error);
        }
    }

    private static void EnsureSupportedPlatform()
    {
        var architecture = RuntimeInformation.ProcessArchitecture;
        var supported = (OperatingSystem.IsWindows() && architecture == Architecture.X64)
            || (OperatingSystem.IsMacOS() && architecture == Architecture.Arm64)
            || (OperatingSystem.IsLinux() && architecture is Architecture.X64 or Architecture.Arm64
                && !RuntimeInformation.RuntimeIdentifier.Contains("musl", StringComparison.Ordinal));
        if (!supported)
            throw new PlatformNotSupportedException($"GraphSpec has no native runtime for {RuntimeInformation.RuntimeIdentifier} ({architecture}). Supported RIDs: win-x64, linux-x64, linux-arm64, osx-arm64.");
    }
}
