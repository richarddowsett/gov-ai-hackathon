# AI Journey Contract Validator (Scala)

## What This Does

This project validates that your **prototype** and **service flow** match a single JSON journey definition.

- Source of truth: `example/journey.json`
- Required schema validation: `schema/journey.schema.json` is enforced at runtime before parsing
- Prototype validation: checks HTML page titles and transitions in `prototype/`
- Service validation: checks route transitions in `service/routes.json`
- Runner output: writes traversal state to `journey-state.json`

The journey model is **index/branch-based** (supports both linear and branching paths):
- Sequential transitions via `_default`
- Conditional transitions via keys like `true`, `false`, or option labels

## Project Structure

- `src/main/scala/contract/` core loader, validators, runner, CLI
- `src/test/scala/JourneyContractSpec.scala` contract tests
- `example/journey.json` journey definition
- `service/routes.json` service-side implementation map
- `prototype/page-*.html` prototype pages
- `scripts/validate-journey` wrapper script
- `scripts/start-play-hmrc.sh` Play server using HMRC scaffold dependencies
- `scripts/start-browser-demo.sh` browser server for prototype + service views
- `scripts/demo-prototype-drift.sh` one-command prototype drift demo
- `scripts/demo-service-drift.sh` one-command service drift demo

## Prerequisites

- Java 11+
- sbt 1.x

## How To Run

Run from repo root:

```bash
cd /Users/nived/code/hackathon/gov-ai-hackathon
```

### 1. Run all tests

```bash
sbt test
```

### 2. Validate journey via CLI wrapper

```bash
./scripts/validate-journey all
```

### 3. Validate only one layer

```bash
./scripts/validate-journey prototype
./scripts/validate-journey service
```

### 4. Run demo scenarios

```bash
./scripts/demo-prototype-drift.sh
./scripts/demo-service-drift.sh
```

### 5. View both in browser

```bash
./scripts/start-play-hmrc.sh
```

Then open:

- `http://127.0.0.1:9000/`
- `http://127.0.0.1:9000/prototype/page-1` (prototype pages)
- `http://127.0.0.1:9000/service` (service transition table)

Fallback option:

- `./scripts/start-browser-demo.sh` runs the older lightweight server on `127.0.0.1:8080`

## Expected Output

On success:

- `✅ prototype validation passed`
- `✅ service validation passed`

On drift, output includes:

- page id
- issue type (`missing`, `mismatch`, `transition`, `extra`)
- expected vs actual values

## How To Update For A New Journey

1. Replace `example/journey.json` with your new journey.
2. Update `service/routes.json` to reflect expected page transitions.
3. Update `prototype/page-*.html` titles and `data-next*` attributes.
4. Run:

```bash
sbt test
./scripts/validate-journey all
```

## Transition Conventions

- Linear page:
  - Service: `"next": { "_default": "page-2" }`
  - Prototype: `<main data-next="page-2">`

- Branching page:
  - Service: `"next": { "true": "page-5", "false": "page-7" }`
  - Prototype: `<main data-next-true="page-5" data-next-false="page-7">`

- End page:
  - Transition target should be `END`
