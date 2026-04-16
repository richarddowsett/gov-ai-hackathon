# AI Journey Contract Validator

Scala-based starter repo for validating a user journey contract across:

- `prototype/` HTML pages
- `service/` implementation metadata via Scala test contract checks

The source of truth is `example/journey.json`, validated by `schema/journey.schema.json`.

This implementation supports:
- Linear transitions via `_default`
- Branch transitions via keys such as `true`, `false`, and option labels

## Quick start

```bash
sbt test
./scripts/validate-journey all
```

## Commands

```bash
./scripts/validate-journey prototype
./scripts/validate-journey service
./scripts/validate-journey all
./scripts/start-browser-demo.sh
./scripts/demo-prototype-drift.sh
./scripts/demo-service-drift.sh
bash demo.sh
```

`ContractLoader` now enforces JSON Schema validation (`schema/journey.schema.json`) before any contract parsing.

## Demo Story (Show This Live)

1. Baseline pass:
```bash
./scripts/validate-journey all
```
2. Break prototype title (expect fail):
```bash
./scripts/demo-prototype-drift.sh
```
3. Break service branch transition (expect fail):
```bash
./scripts/demo-service-drift.sh
```

## Repo layout

```text
.
├── schema/
│   └── journey.schema.json
├── example/
│   └── journey.json
├── prototype/
│   ├── page-1.html
│   ├── ...
│   └── page-9.html
├── service/
│   └── routes.json
├── project/
│   └── build.properties
├── src/main/scala/contract/
│   ├── ContractLoader.scala
│   ├── JourneyRunner.scala
│   ├── PrototypeValidator.scala
│   ├── ServiceValidator.scala
│   ├── Types.scala
│   └── ValidateJourney.scala
├── src/test/scala/
│   └── JourneyContractSpec.scala
├── scripts/
│   ├── validate-journey
│   ├── demo-prototype-drift.sh
│   └── demo-service-drift.sh
├── instruction.md
└── BRIEF.md
```

## Drift detection example

If you change `What is your name?` to `What's your name?` in `prototype/page-2.html`, validation fails with a mismatch for `page-2`.

## Browser Demo

Run:

```bash
./scripts/start-browser-demo.sh
```

Open:
- `http://127.0.0.1:8080/prototype/page-1.html`
- `http://127.0.0.1:8080/service`
