using Neo4j.OntologyGraphSpec.Model;
using Neo4j.OntologyGraphSpec.Validation;

namespace Neo4j.OntologyGraphSpec.Internal;

internal interface IGraphSpecEngine
{
    GraphModel Decode(string content, string format, string sourceType);
    string Encode(GraphModel model, string format, string targetType, string targetVersion);
    IReadOnlyList<Issue> Validate(GraphModel model, IReadOnlyList<ValidationSet> sets);
}
