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

## Releasing the spec

The spec (the JSON schema published to the website) is versioned independently of the libraries and released by merging a version bump to `main`: the Release Spec workflow then creates the `release-spec/vX.Y.Z` tag, which triggers publishing the schema to the website (in TeamCity). Never tag by hand.
The library version needs no bump in the repo: it is derived from the `release/v*` tags by the workflow above.

To release a new spec version, bump all of these in the same PR:

- the `$id` in `ontology-graph-spec.schema.json`
- `Version.LATEST` and `Version.GRAPH_SPEC_VERSIONS` (append the new version) in `Version.kt`
- `ModelVersionGraphSpecLatest` in `go/migration/migration.go`
- a migration from the previous spec version, registered in `defaultConfig` in `GraphSpec.kt`.
  If the format did not change, register `GraphSpecNoOpMigration(from, to)`, which only bumps the version.
  Migrations from `Version.LATEST` to the Aura Data Importer formats must keep working.

This keeps every library release in step with the latest spec.

These are enforced by tests, which fail the Kotlin and Go validation (and so the PR checks) when the versions drift apart:

- the schema `$id` is not `Version.LATEST` (`SpecVersionConsistencyTest`)
- `Version.LATEST` is not the last of `Version.GRAPH_SPEC_VERSIONS`, or a version in it has no migration path to `Version.LATEST` (`GraphSpecVersionsTest`)
- a released `release-spec/v*` tag is missing from `Version.GRAPH_SPEC_VERSIONS` (`SpecVersionConsistencyTest`)
- the schema content has changed but its `$id` version already has a `release-spec/v*` tag, which would leave the published schema out of date (`SpecVersionConsistencyTest`)
- an unreleased version is not higher than the latest released spec version (`SpecVersionConsistencyTest`)
- `ModelVersionGraphSpecLatest` in the Go library is not the schema `$id` version (`TestGraphSpecLatestMatchesSchemaID`)

The tests that look at released versions use the `release-spec/v*` git tags, so they skip when no tags are fetched (CI fetches them).

The Release Spec workflow runs on every push to `main`. If the schema `$id` version has no tag yet, it runs the Kotlin checks and then tags the version.
Merging an unchanged version does nothing.
It is recommended to also protect `release-spec/*`, `release/*` and `go/*` tags with a repository tag ruleset restricting updates and deletions.
