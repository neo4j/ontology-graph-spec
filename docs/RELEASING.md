# Releasing

Releases are automated via the Release workflow, triggered by merging a PR to `main` with one of these labels:

| Label | Effect |
|---|---|
| `release:alpha` | Increment or create the dangling alpha line on the current base |
| `release:rc` | Increment or create the dangling release candidate (rc) line on the current base |
| `release:patch` | Bump patch version (stable) |
| `release:minor` | Bump minor version (stable) |
| `release:major` | Bump major version (stable) |
| `release:promote` | Promote the newest dangling prerelease base to stable (no bump) |

Only one of `release:major`, `release:minor`, `release:patch` may be set at a time.

Combinations:

| Labels | Effect |
|---|---|
| `release:alpha` or `release:rc` alone | Continue that line on the dangling prerelease base if one exists, else create the next patch prerelease |
| prerelease label + bump label | Bump the base, then create or increment the prerelease line on that new base |
| `release:promote` alone | Promote the newest dangling prerelease base to stable |

Prereleases are published as `x.y.z-alpha.N` and `x.y.z-rc.N`, and to npm under the matching `alpha` or `rc` dist-tag.
Stable releases are published under `latest`.
A line only moves forwards (alpha, then rc, then stable): for example `release:alpha` fails once an rc exists on the same base.

Invalid combinations (the workflow fails):

- Both `release:alpha` and `release:rc`
- `release:promote` + any prerelease label
- A prerelease label for an earlier channel than one already released on the base (for example `release:alpha` after an rc)
- `release:promote` + any bump label
- More than one bump label (`release:major` / `release:minor` / `release:patch`)

No manual `npm publish` or Gradle task is needed - just merge a labelled PR.

The workflow pushes the `release/vX.Y.Z` and `go/vX.Y.Z` tags before publishing to npm, Maven Central and NuGet, which makes failures easier to recover from.
The next version is computed from the `release/v*` tags, so pushing them first claims the version.
If tagging fails, nothing is published and the next run computes the same version.
If publishing fails, the version is already claimed and is never computed again.
To recover, rerun only the failed publishing jobs using the same commit, version and retained artifacts, or carry on with the next release. Never move published tags or replace an immutable version with different bits.
Once the Go module proxy has fetched a `go/v*` version, it keeps it even if the tag is deleted, so only reuse a version from the same commit.

The tags are protected by the repository tag ruleset, so the workflow pushes them over SSH with the `RELEASE_TAG_DEPLOY_KEY` secret (see below).

## Releasing the spec

The spec (the JSON schema published to the website) is versioned independently of the libraries and released by merging a version bump to `main`: the Release Spec workflow then creates the `release-spec/vX.Y.Z` tag, which triggers publishing the schema to the website (in TeamCity). Never tag by hand.
The library version needs no bump in the repo: it is derived from the `release/v*` tags by the workflow above.

To release a new spec version, bump all of these in the same PR:

- `Version.LATEST` and `Version.GRAPH_SPEC_VERSIONS` (append the new version) in `Version.kt`
- the schema and Go models, regenerated with `./go/scripts/generate-go-models.sh`, which sets the `$id` in `ontology-graph-spec.schema.json` from `Version.LATEST`; also run `pwsh ./dotnet/scripts/generate-models.ps1 -SkipSchema` to update C# types and schema metadata
- `ModelVersionGraphSpecLatest` in `go/migration/migration.go`
- a migration from the previous spec version, registered in `defaultConfig` in `GraphSpec.kt`.
  If the format did not change, register `GraphSpecNoOpMigration(from, to)`, which only bumps the version.
  Migrations from `Version.LATEST` to the Aura Data Importer formats must keep working.

This keeps every library release in step with the latest spec.

These are enforced by the Kotlin and Go validation, which run on every PR, and fail when the versions drift apart:

- the committed schema or Go models differ from what the Kotlin model generates (the generation check in `validate-go.yaml`)
- the schema `$id` is not `Version.LATEST` (`SpecVersionConsistencyTest`)
- `Version.LATEST` is not the last of `Version.GRAPH_SPEC_VERSIONS`, or a version in it has no migration path to `Version.LATEST` (`GraphSpecVersionsTest`)
- a released `release-spec/v*` tag is missing from `Version.GRAPH_SPEC_VERSIONS` (`SpecVersionConsistencyTest`)
- the schema content has changed but its `$id` version already has a `release-spec/v*` tag, which would leave the published schema out of date (`SpecVersionConsistencyTest`)
- an unreleased version is not higher than the latest released spec version (`SpecVersionConsistencyTest`)
- `ModelVersionGraphSpecLatest` in the Go library is not the schema `$id` version (`TestGraphSpecLatestMatchesSchemaID`)

The tests that look at released versions use the `release-spec/v*` git tags, so they skip when no tags are fetched (CI fetches them).

The Kotlin validation watches Kotlin, Gradle, shared cases, the reference runner and line-ending configuration; and the Go validation skips its checks when a PR only changes docs (`docs/` and Markdown files).
The release workflows pass `check-all` to always run them.

The Release Spec workflow runs on every push to `main`. If the schema `$id` version has no tag yet, it runs the Kotlin checks and then tags the version.
Merging an unchanged version does nothing.
`release-spec/*` tags are protected by a repository tag ruleset, so only the Release Spec workflow creates them.
The workflow pushes the tag over SSH with the `RELEASE_TAG_DEPLOY_KEY` secret, a write deploy key that is on the bypass list of that ruleset.
The Release workflow uses the same key for the `release/*` and `go/*` tags.

## .NET / NuGet

The reusable `validate-dotnet.yaml` builds the native core from the supplied SHA, packs `Neo4j.OntologyGraphSpec`, and
runs the source tests plus an isolated NuGet consumer on Windows x64, Linux x64, Linux ARM64 and macOS ARM64. Linux
ARM64 is cross-compiled on x64 and executed on an ARM64 runner. The workflow also checks schema/Go/C# generation,
Kotlin/Java/JS/Go shared conformance and the documentation preview. It runs on every PR and branch push, so fixture-only
or build-only changes cannot bypass it.

The unified release passes its computed library version into that workflow and waits for all tests before creating
tags. `publish-nuget` downloads the exact `dotnet-package` artifact tested by that run; it does not rebuild the package.
NuGet uses the same `x.y.z`, `x.y.z-alpha.N` or `x.y.z-rc.N` as the other libraries. There is no separate .NET version tag.
The graph specification version continues to come from the generated schema metadata.

Before the first release, a maintainer must establish ownership of the proposed `Neo4j.OntologyGraphSpec` package ID
on nuget.org and configure the repository secret `NUGET_API_KEY` with permission to publish that package. The name is
not claimed by this PR. The release checks that the secret is present before creating tags. Fork PR validation and
local packing require no publishing secrets. Confirm ownership and key scope before applying a release label.

For a failed NuGet publication, rerun the failed job while the original package artifact remains available. Do not
repack an already claimed version from a different commit. If artifact retention has expired, recover the exact
original package from release records or make a new release. Keep successful registries unchanged when recovering a
partial npm/Maven/NuGet release; never delete tags to disguise an already published version.

When native bridge behavior changes, refresh the three checked-in Go native libraries using the existing
`update-native-libs.yaml` workflow (or the native generation script), and commit them with that change. NuGet native
assets are built in CI and are not checked in. Local packages may contain only the developer's staged RID; release
packing requires all four native assets and every consumer test must pass.
