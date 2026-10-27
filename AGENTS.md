# Agent Instructions

## Non-negotiables

- Any change to this repository that would make the docs pages stale MUST update the pages under `docs/` in the same change.
- Any change that alters what the format covers (fields, constraints, cardinality, validation behavior) MUST update the coverage tables in `docs/modules/format/pages/interoperability.adoc` and the interoperability summary in `README.md` in the same change.

## Running and previewing the docs

The documentation is an Antora site living in `docs/`. To work on it:

```bash
cd docs
npm install
npm start
```

`npm start` builds the site and serves it at http://localhost:8000, rebuilding and restarting the server on changes.

Other useful commands (run from `docs/`):

- `npm run build:preview` — build the site once and exit
- `npm run serve` — serve an existing build at http://localhost:8000

Do not use `npm run build` to just build the site: its `postbuild` script starts the server and never exits.
