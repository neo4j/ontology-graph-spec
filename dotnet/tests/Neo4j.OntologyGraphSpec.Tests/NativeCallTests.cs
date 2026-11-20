using System.Text;
using Neo4j.OntologyGraphSpec.Internal;

namespace Neo4j.OntologyGraphSpec.Tests;

public sealed class NativeCallTests
{
    [Fact]
    public void RetriesWithTheRequestedCapacityAndUsesTheReturnedByteLength()
    {
        var payload = Encoding.UTF8.GetBytes(new string('x', 5000) + " café 🌍");
        var calls = 0;
        var actual = NativeCall.Invoke("test", output =>
        {
            calls++;
            if (output.Length < payload.Length + 1) return -(payload.Length + 1);
            Assert.Equal(payload.Length + 1, output.Length);
            output[0] = 0;
            payload.CopyTo(output, 1);
            return payload.Length + 1;
        });
        Assert.Equal(Encoding.UTF8.GetString(payload), actual);
        Assert.Equal(2, calls);
    }

    [Theory]
    [InlineData(-1)]
    [InlineData(int.MinValue)]
    [InlineData(0)]
    [InlineData(4097)]
    [InlineData(-2)]
    [InlineData(-int.MaxValue)]
    public void RejectsInvalidLengthsAndTerminalErrors(int response) =>
        Assert.Throws<GraphSpecException>(() => NativeCall.Invoke("test", _ => response));

    [Fact]
    public void ResizeRetriesAreBounded()
    {
        var calls = 0;
        Assert.Throws<GraphSpecException>(() => NativeCall.Invoke("test", buffer => { calls++; return -(buffer.Length + 1); }));
        Assert.Equal(2, calls);
    }

    [Theory]
    [InlineData(1)]
    [InlineData(99)]
    public void RejectsErrorAndUnknownStatus(byte status) => Assert.Throws<GraphSpecException>(() =>
        NativeCall.Invoke("test", buffer => { buffer[0] = status; buffer[1] = (byte)'!'; return 2; }));

    [Fact]
    public void RejectsInvalidUtf8() => Assert.Throws<GraphSpecException>(() =>
        NativeCall.Invoke("test", buffer => { buffer[0] = 0; buffer[1] = 255; return 2; }));

    [Fact]
    public void LoadingFailureIsNotAValidationSuccess()
    {
        var error = Assert.Throws<GraphSpecException>(() => NativeCall.Invoke("validate", _ => throw new DllNotFoundException("fixture")));
        Assert.IsType<DllNotFoundException>(error.InnerException);
        Assert.Equal("validate", error.Operation);
    }

    [Fact]
    public void IncompatibleLibraryIsReported()
    {
        var error = Assert.Throws<GraphSpecException>(() => NativeCall.Invoke("encode", _ => throw new EntryPointNotFoundException("fixture")));
        Assert.IsType<EntryPointNotFoundException>(error.InnerException);
    }
}
