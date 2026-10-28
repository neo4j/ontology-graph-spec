#!/usr/bin/env bash
# Guards against re-publishing a released spec version with different content.
#
# The spec version lives in the `$id` of ontology-graph-spec.schema.json and in Version.LATEST
# (Version.kt). Spec releases are tags of the form `release-spec/vX.Y.Z`. Checks:
#   1. `$id` version matches Version.LATEST, the last of Version.GRAPH_SPEC_VERSIONS and the Go library's
#      ModelVersionGraphSpecLatest, so every library release supports the latest spec
#      (the Kotlin tests then ensure every listed version has a migration path to LATEST)
#   1b. every released `release-spec/v*` version is listed in Version.GRAPH_SPEC_VERSIONS
#   2. if a `release-spec/v<$id version>` tag exists, the schema must be identical to the tagged one
#   3. otherwise, the version must be higher than every released spec version
# Tags are created by the Release Spec workflow on merge to main, never by hand.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

SCHEMA=ontology-graph-spec.schema.json
GO_MIGRATION=go/migration/migration.go
VERSION_KT=src/commonMain/kotlin/org/neo4j/spec/graph/model/Version.kt
TAG_PREFIX=release-spec/v

fail() { echo "::error::$*" >&2; exit 1; }

id_version() {
  sed -nE 's|.*"\$id": *"https://neo4j.com/docs/ontology-graph-spec/([^/]+)/schema.json".*|\1|p' | head -n1
}

version="$(id_version < "$SCHEMA")"
[[ -n "$version" ]] || fail "Unable to read version from \$id in $SCHEMA"

# Resolve a Version.kt entry that is either a quoted literal or the name of a `const val` string.
resolve() {
  local token="$1"
  if [[ "$token" == \"*\" ]]; then
    echo "${token//\"/}"
  else
    local value
    value="$(sed -nE "s|^[[:space:]]*const val ${token} *= *(.*[^[:space:]]).*|\1|p" "$VERSION_KT" | head -n1)"
    [[ -n "$value" ]] || fail "Unable to resolve $token in $VERSION_KT"
    resolve "$value"
  fi
}

latest_const="$(resolve LATEST)"
[[ "$version" == "$latest_const" ]] ||
  fail "\$id version ($version) in $SCHEMA does not match Version.LATEST ($latest_const) in $VERSION_KT"

go_latest="$(sed -nE 's|.*ModelVersionGraphSpecLatest +ModelVersion *= *"([^"]+)".*|\1|p' "$GO_MIGRATION")"
[[ "$version" == "$go_latest" ]] ||
  fail "\$id version ($version) does not match ModelVersionGraphSpecLatest ($go_latest) in $GO_MIGRATION"

listed=""
for token in $(sed -nE 's|.*val GRAPH_SPEC_VERSIONS *= *listOf\((.*)\).*|\1|p' "$VERSION_KT" | tr -d ' ' | tr ',' ' '); do
  listed+="$(resolve "$token")"$'\n'
done
listed="${listed%$'\n'}"
[[ "$(printf '%s\n' "$listed" | tail -n1)" == "$version" ]] ||
  fail "Version.GRAPH_SPEC_VERSIONS in $VERSION_KT must end with the current spec version ($version)"
while IFS= read -r released; do
  [[ -z "$released" ]] && continue
  printf '%s\n' "$listed" | grep -qxF "${released#"$TAG_PREFIX"}" ||
    fail "Released spec ${released#"$TAG_PREFIX"} is missing from Version.GRAPH_SPEC_VERSIONS in $VERSION_KT. Add it, with a migration (GraphSpecNoOpMigration if nothing changed) to the latest version."
done < <(git tag -l "${TAG_PREFIX}*" || true)

# Normalise away the $id line so only real content changes are compared.
strip_id() { grep -v '"\$id"' ; }

tags="$(git tag -l "${TAG_PREFIX}*" --sort=-v:refname || true)"
if git rev-parse -q --verify "refs/tags/${TAG_PREFIX}${version}" >/dev/null; then
  if ! diff <(git show "${TAG_PREFIX}${version}:$SCHEMA" | strip_id) <(strip_id < "$SCHEMA") >/dev/null; then
    fail "Spec $version is already released (${TAG_PREFIX}${version}) but $SCHEMA has changed since. Bump the spec version (\$id in $SCHEMA and Version.LATEST) so the published schema is not overwritten."
  fi
elif [[ -n "$tags" ]]; then
  highest="$(printf '%s\n' "$tags" | head -n1)"
  highest="${highest#"$TAG_PREFIX"}"
  if [[ "$(printf '%s\n%s\n' "$highest" "$version" | sort -V | tail -n1)" != "$version" || "$highest" == "$version" ]]; then
    fail "Spec version $version must be higher than the latest released spec version $highest."
  fi
fi

echo "Spec version $version OK"
[[ -z "${GITHUB_OUTPUT:-}" ]] || echo "version=$version" >> "$GITHUB_OUTPUT"
