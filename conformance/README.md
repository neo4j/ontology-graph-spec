# Conformance corpus

Language-neutral behaviour cases shared by the Kotlin library and the generated Java library (`java/library`).

`cases.json` lists input documents (referenced in place, not copied) and their type. For each case, every
implementation must produce the files under `expected/<name>/`:

| File | Produced by |
|---|---|
| `graph-spec.json` | decoding the input as its type, then encoding it as the latest graph spec |
| `issues.json` | validating the decoded model with every built-in validator (`Validations.all`) |
| `data-model.json` | encoding the decoded model as a 3.0.0 data model |

Files are compared as JSON values, so formatting and key order don't matter.

The Kotlin library is the source of truth. After an intended behaviour change, regenerate the expected files with:

```bash
./gradlew jvmTest --tests '*ConformanceTest' -PupdateConformance
```

The Java runner (`./gradlew :java:library:test`) then fails until the generated library agrees.
