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

No manual `npm publish`, `cargo publish` or Gradle task is needed - just merge a labelled PR.

The libraries are published to npm, Maven Central and crates.io. npm and crates.io use trusted
publishing: no stored token, the registry mints a short-lived token for the publish job from
GitHub's OIDC identity. Maven Central uses Sonatype credentials from secrets.

### crates.io trusted publishing

The `publish-crates` job authenticates with [crates.io trusted
publishing](https://crates.io/docs/trusted-publishing): the
[`rust-lang/crates-io-auth-action`](https://github.com/rust-lang/crates-io-auth-action) step
exchanges GitHub's OIDC identity for a short-lived crates.io token, which is revoked when the job
ends. No `CARGO_REGISTRY_TOKEN` secret exists to leak.

The name `ontology-graph-spec` is claimed on crates.io, and the trusted publisher for this
repository and the `release.yaml` workflow is registered: every release publishes the crate
through the workflow. The job is gated on the repository variable `CRATES_TRUSTED_PUBLISHING`
(Settings → Secrets and variables → Actions → Variables), currently `true`.

The workflow pushes the `release/vX.Y.Z` and `go/vX.Y.Z` tags before publishing to npm, Maven Central (the Kotlin multiplatform and JVM artifacts `ontology-graph-spec`, plus the native Java port `ontology-graph-spec-java`) and crates.io,
which makes failures easier to recover from.
The next version is computed from the `release/v*` tags, so pushing them first claims the version.
If tagging fails, nothing is published and the next run computes the same version.
If publishing fails, the version is already claimed and is never computed again.
To recover, either delete both tags to reset the version, or carry on with the next release if you are happy to skip a version.
Once the Go module proxy has fetched a `go/v*` version, it keeps it even if the tag is deleted, so only reuse a version from the same commit.

The tags are protected by the repository tag ruleset, so the workflow pushes them over SSH with the `RELEASE_TAG_DEPLOY_KEY` secret (see below).

## Releasing the spec

The spec (the JSON schema published to the website) is versioned independently of the libraries and released by merging a version bump to `main`: the Release Spec workflow then creates the `release-spec/vX.Y.Z` tag, which triggers publishing the schema to the website (in TeamCity). Never tag by hand.
The library version needs no bump in the repo: it is derived from the `release/v*` tags by the workflow above.

To release a new spec version, bump all of these in the same PR:

- `Version.LATEST` and `Version.GRAPH_SPEC_VERSIONS` (append the new version) in `Version.kt`
- the schema and Go models, regenerated with `./go/scripts/generate-go-models.sh`, which sets the `$id` in `ontology-graph-spec.schema.json` from `Version.LATEST`
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

The Kotlin validation skips its checks when a PR changes no Kotlin or Gradle files, and the Go validation skips its checks when a PR only changes docs (`docs/` and Markdown files).
The release workflows pass `check-all` to always run them.

The Release Spec workflow runs on every push to `main`. If the schema `$id` version has no tag yet, it runs the Kotlin checks and then tags the version.
Merging an unchanged version does nothing.
`release-spec/*` tags are protected by a repository tag ruleset, so only the Release Spec workflow creates them.
The workflow pushes the tag over SSH with the `RELEASE_TAG_DEPLOY_KEY` secret, a write deploy key that is on the bypass list of that ruleset.
The Release workflow uses the same key for the `release/*` and `go/*` tags.
