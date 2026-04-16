# AI Journey Contract Validator

Scala + Play HMRC frontend starter for validating a user journey contract across:

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
./scripts/start-json-creator.sh
./scripts/start-play-hmrc.sh
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
├── conf/
│   ├── application.conf
│   └── routes
├── json-creator/
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── app/
│   ├── controllers/
│   │   └── BrowserPlayController.scala
│   ├── services/
│   │   ├── BrowserViewService.scala
│   │   └── ViewModels.scala
│   └── views/
│       ├── main.scala.html
│       ├── home.scala.html
│       ├── prototype.scala.html
│       └── service.scala.html
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
./scripts/start-play-hmrc.sh
```

Open:
- `http://127.0.0.1:9000/`
- `http://127.0.0.1:9000/prototype/page-1`
- `http://127.0.0.1:9000/service`

`start-browser-demo.sh` is still available as a lightweight non-Play fallback on port `8080`.

## Standalone JSON Creator

Run:

```bash
./scripts/start-json-creator.sh
```

Open:
- `http://127.0.0.1:8787/`

This builder is independent from the Play service and contract runtime. It only generates/imports JSON in-browser.
