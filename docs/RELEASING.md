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

The spec (the JSON schema published to the website) is versioned independently of the libraries and released by tagging a commit `release-spec/vX.Y.Z`.
The library version needs no bump in the repo: it is derived from the `release/v*` tags by the workflow above.

The spec version lives in two places that must be bumped together before tagging:

- the `$id` in `ontology-graph-spec.schema.json`
- `Version.LATEST` in `Version.kt`

The Spec Version Guard workflow (`.github/scripts/check-spec-version.sh`) fails when:

- the `$id` version and `Version.LATEST` differ
- the schema content has changed but its version already has a `release-spec/v*` tag (which would overwrite the published schema)
- the version is not higher than the latest released spec version
- a `release-spec/vX.Y.Z` tag is pushed whose version differs from the `$id`
