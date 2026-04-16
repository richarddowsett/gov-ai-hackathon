# AI Journey Contract Validator

A Scala-based tool for validating that GOV.UK prototypes and Play Framework service
implementations conform to an agreed **JSON journey definition** — the single source
of truth between interaction designers and developers.

## Why?

Designs, prototypes, and implementations drift apart. Page titles get changed,
form fields go missing, branching logic is altered. In a world of AI-assisted
development, you need **automated confidence** that what was designed is what was
shipped.

## Architecture

```
                    ┌─────────────────────────┐
                    │   Journey JSON           │
                    │   (source of truth)      │
                    │                          │
                    │   example/journey.json   │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   JourneyParser          │
                    │   (play-json → model)    │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   JourneyGraph           │
                    │   (path enumeration)     │
                    └────────────┬────────────┘
                                 │
               ┌─────────────────┼─────────────────┐
               │                                    │
  ┌────────────▼────────────┐         ┌────────────▼────────────┐
  │   PrototypeValidator     │         │   ServiceValidator       │
  │   (HTML → contract)      │         │   (routes → contract)    │
  │                          │         │                          │
  │   Validates:             │         │   Validates:             │
  │   • page titles          │         │   • route existence      │
  │   • form elements        │         │   • page types           │
  │   • radio/checkbox opts  │         │   • branching routes     │
  │   • question labels      │         │   • title matching       │
  └──────────────────────────┘         └──────────────────────────┘
```

## Prerequisites

- **Java 11+** (Java 17 or 21 recommended)
- **sbt** (Scala Build Tool) — [install guide](https://www.scala-sbt.org/download.html)

That's it. No browser, no Node.js, no Docker required.

## Quick Start

```bash
# Clone the repository
git clone https://github.com/richarddowsett/gov-ai-hackathon.git
cd gov-ai-hackathon/journey-validator

# Run all tests
sbt test
```

## Project Structure

```
journey-validator/
├── build.sbt                              # SBT build definition
├── project/
│   └── build.properties                   # SBT version
│
├── schema/
│   └── journey.schema.json                # JSON Schema for journey definitions
│
├── example/
│   └── journey.json                       # Example journey (9-page survey)
│
├── prototype/
│   ├── page-0-welcome.html                # GOV.UK-styled prototype pages
│   ├── page-1-name.html
│   ├── page-2-dob.html
│   ├── page-3-license.html                # Boolean branching page
│   ├── page-4-vehicle.html                # Radio button branching page
│   ├── page-5-features.html               # Checkbox page
│   ├── page-6-transport.html              # Radio button page
│   ├── page-7-rate.html                   # Multiple questions page
│   └── page-8-thankyou.html               # Terminal content page
│
├── service/
│   └── routes.json                        # Service route descriptor
│
├── src/main/scala/contract/
│   ├── Types.scala                        # Page/Journey case classes
│   ├── JourneyParser.scala                # JSON → model (play-json)
│   ├── JourneyGraph.scala                 # Graph traversal + path enumeration
│   ├── PrototypeValidator.scala           # HTML validation against contract
│   └── ServiceValidator.scala             # Service route validation
│
├── src/test/scala/contract/
│   ├── EmbeddedServer.scala               # Lightweight HTTP server for tests
│   ├── JourneyParserSpec.scala            # Parser unit tests
│   ├── JourneyGraphSpec.scala             # Graph/path enumeration tests
│   ├── PrototypeContractSpec.scala        # Prototype validation (HTTP + HTML)
│   ├── ServiceContractSpec.scala          # Service contract validation
│   └── DriftDetectionSpec.scala           # Demonstrates catching drift
│
├── scripts/
│   ├── validate-journey.sh                # Test runner script
│   └── demo.sh                            # Full demo walkthrough
│
├── BRIEF.md                               # Hackathon brief / pitch
└── README.md                              # This file
```

## The Journey JSON

The source of truth is `example/journey.json`. It defines a 9-page survey with:

| Page | Type | Title | Navigation |
|------|------|-------|------------|
| 0 | contentPage | Welcome to our survey | → 1 |
| 1 | string | What is your name? | → 2 |
| 2 | datePage | What is your date of birth? | → 3 |
| 3 | boolean | Do you have a driver's license? | yes→4, no→6 |
| 4 | radioButton | What type of vehicle do you own? | Car/Moto/Truck→5, Other→6 |
| 5 | checkbox | Select all vehicle features you have: | → 7 |
| 6 | radioButton | What is your preferred transportation method? | all→8 |
| 7 | multipleQuestionsPage | Rate your experience | → end |
| 8 | contentPage | Thank you for completing the survey! | → end |

### Supported Page Types

All 7 page types from the schema are supported:

- **contentPage** — static content, no form
- **string** — text input with optional regex validation
- **datePage** — date input (day/month/year)
- **boolean** — yes/no radio buttons with branching
- **radioButton** — multiple choice with per-option branching
- **checkbox** — multi-select checkboxes
- **multipleQuestionsPage** — multiple questions on one page

### Navigation Model

- **Linear pages** have an integer `index` pointing to the next page's array position
- **Branching pages** have an object `index` mapping each answer to a target page position
- An index pointing beyond the array signals the end of the journey

## Running Tests

### All tests
```bash
sbt test
```

### Individual test suites
```bash
# Parser tests — validates JSON parsing
sbt "testOnly contract.JourneyParserSpec"

# Graph tests — validates path enumeration through branching journeys
sbt "testOnly contract.JourneyGraphSpec"

# Prototype validation — validates HTML pages against the JSON contract
sbt "testOnly contract.PrototypeContractSpec"

# Service validation — validates service routes against the JSON contract
sbt "testOnly contract.ServiceContractSpec"

# Drift detection — demonstrates catching mismatches
sbt "testOnly contract.DriftDetectionSpec"
```

### Using the scripts
```bash
chmod +x scripts/*.sh

./scripts/validate-journey.sh              # run all tests
./scripts/validate-journey.sh prototype    # prototype only
./scripts/validate-journey.sh service      # service only
./scripts/validate-journey.sh drift        # drift detection demo
./scripts/demo.sh                          # full interactive demo
```

## Drift Detection

The key value proposition: when something changes, the validator catches it.

**Example:** Change `What is your name?` to `What's your name?` in the prototype.

The validator reports:

```
FAIL  page[1] title — Expected 'What is your name?', got 'What's your name?'
```

The `DriftDetectionSpec` test suite demonstrates this with 6 scenarios:
- Title mismatch
- Missing form field
- Missing radio option
- Missing service route
- Page type mismatch in service
- Correct page (control case)

## How to Add Your Own Journey

1. Create a new JSON file following `schema/journey.schema.json`
2. Place your prototype HTML files in a directory (named `page-{index}-{slug}.html`)
3. Create a service routes descriptor (`routes.json`)
4. Update the file paths in the test specs (or parameterise them)

## Technology Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Build | SBT 1.10.7 | Standard Scala build tool |
| Language | Scala 2.13 | Type-safe, expressive, Play-compatible |
| JSON parsing | play-json 3.0.4 | Native Play Framework JSON library |
| HTML parsing | Jsoup 1.18.1 | Fast, reliable HTML parser (no browser needed) |
| Testing | ScalaTest 3.2.19 | Standard Scala test framework |
| Test server | Java HttpServer | Zero-dependency embedded server |

## Future Scope

- **Selenium/Playwright browser tests** — for full browser-based validation
- **Cross-framework support** — validate Node.js prototypes, React SPAs
- **Central validation API** — microservice that any team can call
- **AI auto-fix** — detect drift and generate a PR to fix it
- **Visual journey builder** — drag-and-drop UI for creating journey JSONs
- **CI pipeline integration** — fail the build when the contract is violated
