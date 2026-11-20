using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;

namespace Neo4j.OntologyGraphSpec.Internal;

internal static partial class NativeMethods
{
    [LibraryImport("graphdatamodel", EntryPoint = "graphspec_decode", StringMarshalling = StringMarshalling.Utf8)]
    [UnmanagedCallConv(CallConvs = [typeof(CallConvCdecl)])]
    internal static partial int Decode(string content, string format, string sourceType, [Out] byte[] output, int size);

    [LibraryImport("graphdatamodel", EntryPoint = "graphspec_encode", StringMarshalling = StringMarshalling.Utf8)]
    [UnmanagedCallConv(CallConvs = [typeof(CallConvCdecl)])]
    internal static partial int Encode(string model, string format, string targetType, string targetVersion, [Out] byte[] output, int size);

    [LibraryImport("graphdatamodel", EntryPoint = "validate", StringMarshalling = StringMarshalling.Utf8)]
    [UnmanagedCallConv(CallConvs = [typeof(CallConvCdecl)])]
    internal static partial int Validate(string model, string sets, [Out] byte[] output, int size);
}
