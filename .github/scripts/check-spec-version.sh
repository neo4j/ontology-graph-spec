#!/usr/bin/env bash
# Guards against re-publishing a released spec version with different content.
#
# The spec version is the one in the `$id` of ontology-graph-spec.schema.json. Spec releases are tags of the form
# `release-spec/vX.Y.Z`, created by the Release Spec workflow on merge to main, never by hand. Checks:
#   1. if a `release-spec/v<$id version>` tag exists, the schema must be identical to the tagged one
#   2. otherwise, the version must be higher than every released spec version
# Consistency of the version with the libraries (Version.LATEST, Version.GRAPH_SPEC_VERSIONS, the Go constant)
# is checked by the Kotlin and Go tests. Writes `version` to GITHUB_OUTPUT when set.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

SCHEMA=ontology-graph-spec.schema.json
TAG_PREFIX=release-spec/v

fail() { echo "::error::$*" >&2; exit 1; }

version="$(sed -nE 's|.*"\$id": *"https://neo4j.com/docs/ontology-graph-spec/([^/]+)/schema.json".*|\1|p' "$SCHEMA" | head -n1)"
[[ -n "$version" ]] || fail "Unable to read version from \$id in $SCHEMA"

# Normalise away the $id line so only real content changes are compared.
strip_id() { grep -v '"\$id"' ; }

tags="$(git tag -l "${TAG_PREFIX}*" --sort=-v:refname)"
if git rev-parse -q --verify "refs/tags/${TAG_PREFIX}${version}" >/dev/null; then
  if ! diff <(git show "${TAG_PREFIX}${version}:$SCHEMA" | strip_id) <(strip_id < "$SCHEMA") >/dev/null; then
    fail "Spec $version is already released (${TAG_PREFIX}${version}) but $SCHEMA has changed since. Bump the spec version (\$id in $SCHEMA and Version.LATEST) so the published schema is not overwritten."
  fi
elif [[ -n "$tags" ]]; then
  highest="$(printf '%s\n' "$tags" | head -n1)"
  highest="${highest#"$TAG_PREFIX"}"
  if [[ "$(printf '%s\n%s\n' "$highest" "$version" | sort -V | tail -n1)" != "$version" ]]; then
    fail "Spec version $version must be higher than the latest released spec version $highest."
  fi
fi

echo "Spec version $version OK"
[[ -z "${GITHUB_OUTPUT:-}" ]] || echo "version=$version" >> "$GITHUB_OUTPUT"
