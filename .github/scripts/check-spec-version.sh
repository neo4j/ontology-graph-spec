#!/usr/bin/env bash
# Guards against re-publishing a released spec version with different content.
#
# The spec version lives in the `$id` of ontology-graph-spec.schema.json and in Version.LATEST
# (Version.kt). Spec releases are tags of the form `release-spec/vX.Y.Z`. Checks:
#   1. `$id` version matches Version.LATEST
#   2. if a `release-spec/v<$id version>` tag exists, the schema must be identical to the tagged one
#   3. otherwise, the version must be higher than every released spec version
#   4. with SPEC_TAG set (tag builds), the tag version must equal the `$id` version
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

SCHEMA=ontology-graph-spec.schema.json
VERSION_KT=src/commonMain/kotlin/org/neo4j/spec/graph/model/Version.kt
TAG_PREFIX=release-spec/v

fail() { echo "::error::$*" >&2; exit 1; }

id_version() {
  sed -nE 's|.*"\$id": *"https://neo4j.com/docs/ontology-graph-spec/([^/]+)/schema.json".*|\1|p' | head -n1
}

version="$(id_version < "$SCHEMA")"
[[ -n "$version" ]] || fail "Unable to read version from \$id in $SCHEMA"

latest_const="$(sed -nE 's|.*const val LATEST = "([^"]+)".*|\1|p' "$VERSION_KT")"
[[ "$version" == "$latest_const" ]] ||
  fail "\$id version ($version) in $SCHEMA does not match Version.LATEST ($latest_const) in $VERSION_KT"

if [[ -n "${SPEC_TAG:-}" ]]; then
  [[ "${SPEC_TAG#"$TAG_PREFIX"}" == "$version" ]] ||
    fail "Tag $SPEC_TAG does not match the schema \$id version ($version). Bump the version before tagging."
fi

# Normalise away the $id line so only real content changes are compared.
strip_id() { grep -v '"\$id"' ; }

tags="$(git tag -l "${TAG_PREFIX}*" --sort=-v:refname | grep -vxF "${SPEC_TAG:-}" || true)"
if git rev-parse -q --verify "refs/tags/${TAG_PREFIX}${version}" >/dev/null && [[ "${SPEC_TAG:-}" != "${TAG_PREFIX}${version}" ]]; then
  if ! diff <(git show "${TAG_PREFIX}${version}:$SCHEMA" | strip_id) <(strip_id < "$SCHEMA") >/dev/null; then
    fail "Spec $version is already released (${TAG_PREFIX}${version}) but $SCHEMA has changed since. Bump the spec version (\$id in $SCHEMA and Version.LATEST) so the published schema is not overwritten."
  fi
elif [[ -z "${SPEC_TAG:-}" && -n "$tags" ]]; then
  highest="$(printf '%s\n' "$tags" | head -n1)"
  highest="${highest#"$TAG_PREFIX"}"
  if [[ "$(printf '%s\n%s\n' "$highest" "$version" | sort -V | tail -n1)" != "$version" || "$highest" == "$version" ]]; then
    fail "Spec version $version must be higher than the latest released spec version $highest."
  fi
fi

echo "Spec version $version OK"
