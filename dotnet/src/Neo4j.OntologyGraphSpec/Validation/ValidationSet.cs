namespace Neo4j.OntologyGraphSpec.Validation;

/// <summary>Built-in sets, composed and deduplicated by the shared Kotlin validator.</summary>
public enum ValidationSet
{
    Core,
    Labelled,
    ConsistentSchema,
    Mappable,
    BulkImportable,
    WellConstrained,
    All
}
