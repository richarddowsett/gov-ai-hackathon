# AI Journey Contract Validator

Scala-based starter repo for validating a user journey contract across:

- `prototype/` HTML pages
- `service/` implementation metadata via Scala test contract checks

The source of truth is `example/journey.json`, validated by `schema/journey.schema.json`.

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
bash demo.sh
```

## Repo layout

```text
.
├── schema/
│   └── journey.schema.json
├── example/
│   └── journey.json
├── prototype/
│   ├── start.html
│   ├── email.html
│   └── confirm.html
├── service/
│   └── routes.json
├── project/
│   └── build.properties
├── src/test/scala/
│   └── JourneyContractSpec.scala
├── src/main/scala/contract/
│   ├── ContractLoader.scala
│   ├── JourneyRunner.scala
│   ├── PrototypeValidator.scala
│   ├── ServiceValidator.scala
│   ├── Types.scala
│   └── ValidateJourney.scala
├── scripts/
│   └── validate-journey
└── BRIEF.md
```

## Drift detection example

If you change `Enter your name` to `What's your name?` in `prototype/start.html`, validation fails with a mismatch for page `start`.
