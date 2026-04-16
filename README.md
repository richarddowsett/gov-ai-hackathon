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
- **Python 3** (only needed for the prototype web server, not for tests)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/richarddowsett/gov-ai-hackathon.git
cd gov-ai-hackathon/journey-validator

# Run all tests
sbt test
```

## Browsing the Prototype

The prototype pages can be viewed in a real browser with full GOV.UK styling,
working form submissions, and branching logic driven by the journey JSON.

```bash
python3 prototype/server.py
```

Then open **http://localhost:4000** in your browser.

Features:
- **GOV.UK styling** — header, phase banner, styled form elements, green action buttons
- **Working branching** — boolean and radio button pages route you down different paths based on your selection, exactly as defined in `example/journey.json`
- **Back links** — every page after the first has a "Back" link
- **Page navigator** — a debug strip at the bottom of every page showing all 9 pages with direct links (current page highlighted)
- **Custom port** — pass a port number as an argument: `python3 prototype/server.py 8080`

Try both journey paths:
1. Answer **Yes** to "Do you have a driver's license?" → vehicle type → features → rate experience
2. Answer **No** → preferred transportation method → thank you

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
│   ├── server.py                          # Browsable prototype server (python3)
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
│   ├── ValidatorConfig.scala              # Configurable paths (system properties)
│   ├── EmbeddedServer.scala               # Lightweight HTTP server for tests
│   ├── JourneyContractSpec.scala          # JSON-driven contract validation
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

The validator is **fully dynamic** — you specify the journey JSON, prototype
directory, and service descriptor and it validates everything automatically.
Nothing is hardcoded to a particular journey.

### Default (uses the example journey shipped with this repo)

```bash
sbt test
```

### Custom journey — system properties

Pass `-D` flags to point at your own files:

```bash
sbt \
  -Djourney.json=path/to/your/journey.json \
  -Dprototype.dir=path/to/your/prototype \
  -Dservice.json=path/to/your/routes.json \
  test
```

### Custom journey — shell script

The runner script accepts `--journey`, `--prototype`, and `--service` flags,
or environment variables:

```bash
chmod +x scripts/*.sh

# flags
./scripts/validate-journey.sh \
  --journey my/journey.json \
  --prototype my/proto \
  --service my/routes.json

# environment variables
JOURNEY_JSON=my/journey.json \
PROTOTYPE_DIR=my/proto \
SERVICE_JSON=my/routes.json \
./scripts/validate-journey.sh
```

### Individual suites

```bash
sbt "testOnly contract.JourneyContractSpec"   # schema, graph, prototype, service
sbt "testOnly contract.DriftDetectionSpec"     # drift detection demo

# or via the script
./scripts/validate-journey.sh contract
./scripts/validate-journey.sh drift
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
4. Run the validator against your files — no code changes needed:

```bash
sbt \
  -Djourney.json=your/journey.json \
  -Dprototype.dir=your/prototype \
  -Dservice.json=your/routes.json \
  test
```

## Technology Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Build | SBT 1.10.7 | Standard Scala build tool |
| Language | Scala 2.13 | Type-safe, expressive, Play-compatible |
| JSON parsing | play-json 3.0.4 | Native Play Framework JSON library |
| HTML parsing | Jsoup 1.18.1 | Fast, reliable HTML parser (no browser needed) |
| Testing | ScalaTest 3.2.19 | Standard Scala test framework |
| Test server | Java HttpServer | Zero-dependency embedded server |
| Prototype server | Python 3 http.server | Browse the journey in a real browser |

## Future Scope

- **Selenium/Playwright browser tests** — for full browser-based validation
- **Cross-framework support** — validate Node.js prototypes, React SPAs
- **Central validation API** — microservice that any team can call
- **AI auto-fix** — detect drift and generate a PR to fix it
- **Visual journey builder** — drag-and-drop UI for creating journey JSONs
- **CI pipeline integration** — fail the build when the contract is violated
