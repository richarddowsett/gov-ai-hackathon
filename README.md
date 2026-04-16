# AI Journey Contract Validator

A full-stack platform for defining, storing, and validating GOV.UK user journeys.
An interaction designer defines a journey in JSON; validation libraries for both
Scala (Play Framework) and Python (prototypes) catch drift automatically — every
page title, every form field, every branching path.

---

## Problem Statement

### The drift problem in government digital services

Government digital services follow a well-defined lifecycle: an interaction designer
designs a user journey, a prototype is built for user testing, and a developer
implements the production service. At every handoff, information can be lost or
changed. A page title is rephrased, a form field is removed, a branching path is
wired differently.

This drift is hard to catch. The prototype and the service are built with different
technologies (GOV.UK Prototype Kit HTML vs Scala Play), by different people, at
different times. There is no automated way to check that they agree.

### AI makes it worse

In a world where AI agents generate code at speed, drift accelerates. An AI assistant
can refactor a prototype or scaffold a service in seconds — but it has no awareness of
the original design contract. You get faster delivery with less confidence that the
output is correct.

### What we need

A **machine-readable contract** that captures the agreed user journey, and
**automated validation** that checks both the prototype and the service against it —
continuously, in CI, before every merge.

---

## Solution

### End-to-end architecture

```
┌─────────────────────────┐
│  JSON Creator UI        │  Interaction designer builds the journey
│  (browser app)          │  via a schema-driven form
│  http://localhost:8787  │
└────────────┬────────────┘
             │  POST /journeys
             ▼
┌─────────────────────────┐
│  Journey Storage API    │  Stores journey JSON by service name
│  (Play Framework)       │  in PostgreSQL
│  http://localhost:9000  │
└────────────┬────────────┘
             │  GET /journeys/:serviceName
     ┌───────┴───────┐
     ▼               ▼
┌──────────┐  ┌──────────────┐
│ Prototype│  │ Play Service  │  Both consume the same journey JSON
│ (HTML)   │  │ (example-     │
│          │  │  service)     │
└────┬─────┘  └──────┬───────┘
     │               │
     ▼               ▼
┌──────────┐  ┌──────────────┐
│prototype-│  │journey-      │  Validation libraries catch drift
│validation│  │validation    │  against the contract
│(Python)  │  │(Scala)       │
└──────────┘  └──────────────┘
```

### What gets validated

| Layer | Checks performed |
|-------|-----------------|
| **Schema structure** | Every page has a non-empty title, a recognised type, valid indices, and branching pages define routes |
| **Graph integrity** | At least one path exists, all paths start at page 0, no infinite loops |
| **Play service (Scala)** | Each page renders with correct `<h1>` title, correct form elements, and form submissions navigate to the right next page for every branching answer |
| **Prototype (Python)** | Each HTML page has the correct `<h1>` title, form elements matching the page type, and correct option labels |
| **Drift detection** | Any change to any of the above is caught and reported immediately |

### Failure output

When something drifts, the tests tell you exactly what went wrong:

```
FAILED  page[1] (string) 'What is your name?' should render correctly
  - Expected <h1> to contain "What is your name?" but found "What's your name?"
```

---

## Quick Start

### Prerequisites

- **Docker** and **Docker Compose** (for the full stack)
- **Java 11+** (Java 17 or 21 recommended, for sbt)
- **sbt** (Scala Build Tool) — [install guide](https://www.scala-sbt.org/download.html)
- **Python 3** (for prototype server and prototype-validation)

### Run everything with Docker Compose

```bash
./scripts/docker-up.sh
```

This starts:

| Service | URL | Description |
|---------|-----|-------------|
| JSON Creator | http://localhost:8787 | Browser UI for designing journeys |
| Prototype | http://localhost:4000 | Browsable GOV.UK-styled prototype |
| Journey Storage API | http://localhost:9000/journeys | REST API for journey persistence |
| PostgreSQL | localhost:5433 | Database for journey storage |

If port 5433 is taken:

```bash
POSTGRES_PORT=55432 docker compose up -d
```

Stop everything:

```bash
./scripts/docker-down.sh
```

### Run validation tests

```bash
# Validate the Play service against journey.json
sbt "exampleService/test"

# Validate the prototype against journey.json
cd prototype && python3 -m pytest tests/ -v

# Run the Play service locally (browse at http://localhost:9000/start)
sbt "exampleService/run"
```

---

## Components

### journey-storage (Play Framework API)

A Play Framework microservice that stores and retrieves journey JSON definitions
keyed by service name. Backed by PostgreSQL with automatic schema evolution.

**REST API:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/journeys` | List all stored journeys |
| `POST` | `/journeys` | Create a new journey |
| `GET` | `/journeys/:serviceName` | Get journey by service name |
| `PUT` | `/journeys/:serviceName` | Update an existing journey |
| `DELETE` | `/journeys/:serviceName` | Delete a journey |

**Example:**

```bash
# Store a journey
curl -X POST http://localhost:9000/journeys \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"my-service","json":"{\"pages\":[...]}"}'

# Retrieve it
curl http://localhost:9000/journeys/my-service
```

See [instruction.md](instruction.md) for detailed setup instructions.

### journey-validation (Scala library)

A reusable Scala library that any Play Framework service can depend on to validate
itself against a journey JSON contract. Provides:

- **Model classes** — `Journey`, `Page`, `PageIndex` (linear and branching)
- **JSON parser** — reads journey JSON into the typed model via play-json
- **Graph engine** — DFS enumeration of all valid paths through the journey
- **Page validator** — checks rendered HTML (via Jsoup) for correct titles, form
  elements, radio/checkbox options, and question labels
- **`JourneySpec` trait** — mix into your test suite to get auto-generated tests for
  every page and every path

**Usage in a Play service:**

```scala
class JourneyValidationSpec extends JourneySpec with GuiceOneAppPerSuite {
  lazy val journeyJson: String = {
    val stream = app.classloader.getResourceAsStream("journey.json")
    scala.io.Source.fromInputStream(stream).mkString
  }

  validatePages()   // generates a test for each page
  validatePaths()   // generates a test for each unique path through the journey
}
```

See [example-service/README.md](example-service/README.md) for the full example.

### example-service (Play Framework app)

A working Play Framework application that dynamically renders pages based on
`journey.json`. Demonstrates how to use the `journey-validation` library. The
controller reads the journey at startup and renders the appropriate Twirl template
for each page type, with working form submissions and branching navigation.

### prototype-validation (Python library)

The Python equivalent of `journey-validation`, for validating HTML prototypes:

- **Model classes** — `Journey`, `Page`, `Question`, `Pass`, `Fail`
- **JSON parser** — reads journey JSON into Python dataclasses
- **Graph engine** — path enumeration matching the Scala implementation
- **Page validator** — checks HTML files (via BeautifulSoup4) for correct structure
- **`PrototypeTestSuite`** — high-level test suite class for use with pytest

**Usage with pytest:**

```python
@pytest.mark.parametrize("page_index", range(len(suite.journey.pages)))
def test_page(suite, page_index):
    report = suite.validate_page(page_index)
    assert report.all_passed, report.summary()
```

See [prototype-validation/README.md](prototype-validation/README.md) for details.

### json-creator (browser app)

A standalone browser-based tool for interaction designers to create journey JSON
files. Provides a form-driven UI based on `journey.schema.json`, with integration
into the Journey Storage API for saving and loading journeys.

### prototype (HTML pages)

GOV.UK-styled static HTML pages representing the user journey. Served by a simple
Python HTTP server with working form submissions and branching logic.

---

## The Journey JSON

The source of truth is a JSON file conforming to `schema/journey.schema.json`. The
example journey (`example/journey.json`) defines a 9-page survey:

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

This produces **3 unique paths** through the journey:

1. Welcome → Name → DOB → License (yes) → Vehicle (Car/Moto/Truck) → Features → Rate experience
2. Welcome → Name → DOB → License (yes) → Vehicle (Other) → Transport method → Thank you
3. Welcome → Name → DOB → License (no) → Transport method → Thank you

### Supported page types

| Type | Description | Validated elements |
|------|-------------|--------------------|
| `contentPage` | Static content, no form | `<h1>` title only |
| `string` | Text input | `<h1>` title, `<input type="text">` |
| `datePage` | Date input (day/month/year) | `<h1>` title, date input elements |
| `boolean` | Yes/no with branching | `<h1>` title, 2+ `<input type="radio">` |
| `radioButton` | Multiple choice with per-option branching | `<h1>` title, radio count, each option label |
| `checkbox` | Multi-select | `<h1>` title, checkbox count, each option label |
| `multipleQuestionsPage` | Multiple questions on one page | `<h1>` title, each question label |

### Navigation model

- **Linear pages** have an integer `index` pointing to the next page's array position
- **Branching pages** have an object `index` mapping each answer string to a target page index
- An index pointing beyond the array length signals the end of the journey

---

## Running Tests

### Play service validation (Scala)

```bash
# All tests (journey-validation library + example-service)
sbt test

# Just the example service
sbt "exampleService/test"
```

The `JourneySpec` trait dynamically generates tests from `journey.json` — 9 page
validation tests and 3 path traversal tests for the example journey (16 total with
edge cases).

### Prototype validation (Python)

```bash
cd prototype
python3 -m venv .venv
source .venv/bin/activate
pip install -r tests/requirements.txt
python3 -m pytest tests/ -v
```

This runs BeautifulSoup-based checks against every HTML page and every path.

### Drift detection demo

```bash
chmod +x scripts/demo.sh
./scripts/demo.sh
```

The demo:
1. Shows the journey from `journey.json`
2. Validates the Play service (should pass)
3. Introduces drift — changes a Twirl template to hide a form field
4. Re-validates (should fail with a clear error)
5. Reverts the change

---

## Project Structure

```
journey-validator/
├── build.sbt                              # Multi-project SBT build
├── docker-compose.yml                     # Full stack: json-creator, prototype,
│                                          #   journey-storage, postgres
├── project/
│   ├── build.properties                   # SBT 1.10.7
│   └── plugins.sbt                        # Play Framework SBT plugin
│
├── journey-validation/                    # Scala validation library
│   └── src/main/scala/journeyvalidation/
│       ├── model.scala                    #   Page, Journey, PageIndex types
│       ├── JourneyParser.scala            #   JSON → Journey (play-json)
│       ├── JourneyGraph.scala             #   DFS path enumeration
│       ├── PageValidator.scala            #   HTML checks (Jsoup)
│       └── JourneySpec.scala              #   ScalaTest trait for Play apps
│
├── example-service/                       # Example Play Framework app
│   ├── app/
│   │   ├── controllers/
│   │   │   └── JourneyController.scala    #   Dynamic page rendering
│   │   └── views/                         #   Twirl templates (GOV.UK styled)
│   ├── conf/
│   │   ├── application.conf
│   │   ├── routes
│   │   └── journey.json                   #   Journey contract (classpath)
│   └── test/scala/
│       └── JourneyValidationSpec.scala    #   Uses JourneySpec trait
│
├── journey-storage/                       # Journey persistence API (Play)
│   ├── app/
│   │   ├── controllers/
│   │   │   └── JourneyController.scala    #   REST CRUD endpoints
│   │   ├── models/
│   │   │   └── Journey.scala              #   (serviceName, json) model
│   │   └── repositories/
│   │       └── JourneyRepository.scala    #   PostgreSQL via JDBC
│   └── conf/
│       ├── application.conf               #   DB config, CORS, evolutions
│       ├── routes                         #   /journeys/:serviceName
│       └── evolutions/default/1.sql       #   Schema migration
│
├── prototype-validation/                  # Python validation library
│   ├── pyproject.toml
│   └── journeyvalidation/
│       ├── model.py                       #   Page, Journey dataclasses
│       ├── parser.py                      #   JSON → Journey
│       ├── graph.py                       #   Path enumeration
│       ├── page_validator.py              #   HTML checks (BeautifulSoup4)
│       └── prototype_suite.py             #   PrototypeTestSuite class
│
├── json-creator/                          # Browser-based journey designer
│   ├── index.html
│   ├── app.js
│   ├── styles.css
│   └── journey.schema.json
│
├── prototype/                             # GOV.UK-styled HTML prototype
│   ├── server.py                          #   Python HTTP server (port 4000)
│   ├── page-0-welcome.html … page-8-thankyou.html
│   └── tests/                             #   Pytest suite using prototype-validation
│       ├── conftest.py
│       ├── test_journey_validation.py
│       └── requirements.txt
│
├── example/
│   └── journey.json                       # Example journey definition
├── schema/
│   └── journey.schema.json                # JSON Schema for journeys
├── scripts/
│   ├── docker-up.sh                       # Start full Docker stack
│   ├── docker-down.sh                     # Stop Docker stack
│   ├── start-json-creator.sh              # Start json-creator standalone
│   ├── validate-journey.sh                # Run validation tests
│   └── demo.sh                            # Interactive drift detection demo
│
├── README.md                              # This file
├── BRIEF.md                               # Hackathon pitch
├── Diagarm.md                             # Architecture diagram
├── End2End.md                             # End-to-end flow diagram
└── instruction.md                         # Docker / JSON Creator setup guide
```

## SBT Build Structure

The project uses a multi-project SBT build with three subprojects:

| Project | Directory | Description |
|---------|-----------|-------------|
| `journeyValidation` | `journey-validation/` | Scala validation library (published artifact) |
| `exampleService` | `example-service/` | Play app demonstrating the library |
| `storageService` | `journey-storage/` | Play API for journey persistence |
| `root` | `.` | Aggregates all subprojects |

```bash
sbt "journeyValidation/compile"   # compile the library
sbt "exampleService/test"         # test the example service
sbt "storageService/run"          # run the storage API (needs PostgreSQL)
sbt compile                       # compile everything
```

## Technology Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Build | SBT 1.10.7 | Standard Scala build tool |
| Language (service) | Scala 2.13 | Type-safe, Play-compatible |
| Language (prototype validation) | Python 3 | Matches prototype ecosystem |
| Service framework | Play Framework 3.0.6 | Standard GOV.UK service framework |
| JSON parsing | play-json 3.0.4 | Native Play JSON library |
| HTML parsing (Scala) | Jsoup 1.18.1 | Fast static HTML parser |
| HTML parsing (Python) | BeautifulSoup4 | Standard Python HTML parser |
| Testing (Scala) | ScalaTest + ScalaTestPlusPlay | Play-integrated test framework |
| Testing (Python) | pytest | Standard Python test framework |
| Database | PostgreSQL 16 | Journey storage persistence |
| Containerisation | Docker Compose | Run the full stack locally |

## Future Scope

- Fetch journey JSON from the storage API at test time (instead of local file)
- Published `journey-validation` library on Maven Central
- Published `prototype-validation` package on PyPI
- CI pipeline integration — fail the build when the contract is violated
- AI auto-fix — detect drift and generate a PR to fix it
- Cross-framework support — Node.js, React SPAs
- Schema evolution — versioned journey contracts with migration support
