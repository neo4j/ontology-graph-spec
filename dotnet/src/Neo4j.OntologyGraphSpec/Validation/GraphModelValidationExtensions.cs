using Neo4j.OntologyGraphSpec.Internal;
using Neo4j.OntologyGraphSpec.Model;

namespace Neo4j.OntologyGraphSpec.Validation;

public static class GraphModelValidationExtensions
{
    /// <summary>Validate against the union of the selected sets. Defaults to Core when no sets are supplied.</summary>
    public static IReadOnlyList<Issue> Validate(this GraphModel model, params ValidationSet[] sets)
    {
        ArgumentNullException.ThrowIfNull(model);
        ArgumentNullException.ThrowIfNull(sets);
        return NativeGraphSpecEngine.Instance.Validate(model, sets);
    }
}
