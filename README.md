# AI Journey Contract Validator

A Scala-based tool for validating that GOV.UK prototypes and Play Framework service
implementations conform to an agreed **JSON journey definition** — the single source
of truth between interaction designers and developers.

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

### The journey JSON as single source of truth

The interaction designer and the developer agree on a journey defined in a single
JSON file. This file captures every page in the journey: its type, title, form
fields, options, branching logic, and navigation order.

From this one file, the validator automatically:

1. **Parses** the JSON into a typed Scala model
2. **Builds a graph** and enumerates every valid path through the journey (including all branching combinations)
3. **Validates the prototype** — fetches each HTML page and checks that it has the correct title, form elements, radio/checkbox options, and question labels
4. **Validates the service** — checks that the service route descriptor defines routes for every page with correct types, titles, and branching routes
5. **Reports failures** with exact file and line references so you know precisely what to fix

### What gets validated

| Layer | Checks performed |
|-------|-----------------|
| **Schema structure** | Every page has a non-empty title, a recognised type, valid indices, and branching pages define routes |
| **Graph integrity** | At least one path exists, all paths start at page 0, no infinite loops |
| **Prototype HTML** | `<h1>` title matches contract, correct form elements for the page type (text inputs, date fields, radio buttons, checkboxes, question labels), all options present with matching labels |
| **Service routes** | Every journey page has a corresponding route, page types match, branching routes cover all answers |
| **Drift detection** | Any change to any of the above is caught and reported with the expected value, the actual value, the contract line, and the file:line to fix |

### Failure output

When something drifts, the output tells you exactly where to look:

```
❌  page[1] — title
      expected : 'What is your name?'
      actual   : 'What's your name?'
      contract : example/journey.json:10
      fix at   : prototype/page-1-name.html:13
```

- **contract** points to the line in the journey JSON that defines the expectation
- **fix at** points to the line in the prototype or service file that needs updating

---

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
                    │   (DFS path enumeration) │
                    └────────────┬────────────┘
                                 │
               ┌─────────────────┼─────────────────┐
               │                                    │
  ┌────────────▼────────────┐         ┌────────────▼────────────┐
  │   PrototypeValidator     │         │   ServiceValidator       │
  │   (Jsoup HTML parsing)   │         │   (play-json routes)     │
  │                          │         │                          │
  │   Validates:             │         │   Validates:             │
  │   • <h1> title           │         │   • route existence      │
  │   • form elements        │         │   • page type match      │
  │   • radio/checkbox opts  │         │   • branching routes     │
  │   • question labels      │         │   • title match          │
  └────────┬─────────────────┘         └────────┬─────────────────┘
           │                                    │
           └─────────────┬──────────────────────┘
                         │
              ┌──────────▼──────────┐
              │   SourceLocator      │
              │   (file:line refs)   │
              └─────────────────────┘
```

### Data flow

1. `JourneyParser` reads the JSON file and produces a `Journey` model containing a `Vector[Page]`. Each `Page` has a polymorphic `PageIndex` — either `LinearIndex(next: Int)` for sequential pages, or `BranchingIndex(routes: Map[String, Int])` for boolean/radio pages where each answer leads to a different page.

2. `JourneyGraph` performs a depth-first walk from page 0. At each branching page it forks into multiple paths. Cycle detection prevents infinite recursion. The result is a `List[JourneyPath]`, each being a complete start-to-terminal route through the journey.

3. `PrototypeValidator` takes each page's contract and the corresponding HTML (fetched from an embedded HTTP server serving the prototype directory). It uses Jsoup to parse the DOM and checks: title text from `<h1>`, form element presence by type (`input[type=text]`, `input[type=radio]`, etc.), option labels from `<label>` elements, and question titles from labels/legends.

4. `ServiceValidator` takes each page's contract and a parsed `ServiceDescriptor` (a list of routes from a JSON file). It checks that a route exists for every page, that `pageType` matches, and that branching pages define `nextRoutes` for every answer in the contract.

5. `SourceLocator` is called when a check fails. It reads the raw source files and searches for the relevant content to resolve line numbers. This produces `SourceRef(file, line)` values that are attached to `Fail` results, giving developers an exact pointer to what needs changing.

---

## Implementation Details

### Core model (`Types.scala`)

```scala
sealed trait PageIndex
case class LinearIndex(next: Int)                   extends PageIndex
case class BranchingIndex(routes: Map[String, Int]) extends PageIndex

case class Page(
    pageType:   String,               // contentPage, string, datePage, boolean, radioButton, checkbox, multipleQuestionsPage
    title:      String,               // the <h1> heading — primary validation anchor
    index:      PageIndex,            // where to navigate next
    options:    List[String],         // radio/checkbox option labels
    questions:  List[Question],       // sub-questions for multipleQuestionsPage
    validation: Option[List[String]]  // optional regex patterns
)

case class Journey(pages: Vector[Page])
```

The `index` field is polymorphic in the JSON: an integer for linear pages, an object
for branching pages. `JourneyParser` handles this with a custom `Reads[PageIndex]`
that tries `Int` first, then falls back to `Map[String, Int]`.

### Validation result model (`PrototypeValidator.scala`)

```scala
sealed trait ValidationResult { def check: String; def isPass: Boolean }
case class Pass(check: String) extends ValidationResult
case class Fail(
    check:       String,
    expected:    String,
    actual:      String,
    contractRef: Option[SourceRef] = None,  // journey.json:10
    sourceRef:   Option[SourceRef] = None   // prototype/page-1-name.html:13
) extends ValidationResult
```

Every check produces either a `Pass` or a `Fail`. Failures carry optional source
references so the output can point directly to the lines that need attention.

### Configuration (`ValidatorConfig.scala`)

All file paths are driven by system properties with sensible defaults:

| Property | Default | Description |
|----------|---------|-------------|
| `journey.json` | `example/journey.json` | Path to the journey contract JSON |
| `prototype.dir` | `prototype` | Directory containing prototype HTML files |
| `service.json` | `service/routes.json` | Path to the service route descriptor |

This means you can validate **any journey** without changing a single line of code —
just pass different `-D` flags to sbt.

### Test structure

There are two test suites:

- **`JourneyContractSpec`** — the main validation suite. Dynamically generates tests
  from the journey JSON: schema structure checks, graph path enumeration, per-page
  prototype validation, full-path prototype traversal, and service route validation.
  Everything is discovered from the JSON — no hardcoded page titles, indices, or options.

- **`DriftDetectionSpec`** — demonstrates that the validator catches specific kinds
  of drift by constructing deliberately broken HTML and incomplete service descriptors.
  Finds pages by type from the JSON (e.g. "give me a `radioButton` page") rather than
  hardcoding indices.

### Embedded HTTP server (`EmbeddedServer.scala`)

Tests need to fetch prototype pages over HTTP (mimicking how a real prototype server
works). `EmbeddedServer` uses Java's built-in `com.sun.net.httpserver.HttpServer` with
zero dependencies. It maps `GET /page/{index}` to files matching
`page-{index}-*.html` in the prototype directory, reading them fresh from disk on
every request.

### Prototype naming convention

Prototype HTML files must follow the pattern `page-{index}-{slug}.html` where
`{index}` is the zero-based array position in the journey JSON and `{slug}` is a
human-readable name. For example:

```
page-0-welcome.html
page-1-name.html
page-3-license.html
```

The slug is for developer convenience only — the validator matches files by index.

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

All 7 page types from the schema are supported:

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

### Service route descriptor

The service descriptor (`service/routes.json`) describes the routes a Play Framework
service exposes. Each route has:

```json
{
  "method": "GET",
  "path": "/drivers-license",
  "pageType": "boolean",
  "title": "Do you have a driver's license?",
  "nextRoutes": {
    "true": "/vehicle-type",
    "false": "/transport-method"
  }
}
```

The validator checks that every page in the journey has a matching route with the
correct `pageType`, `title`, and (for branching pages) `nextRoutes` covering all
answers.

---

## Prerequisites

- **Java 11+** (Java 17 or 21 recommended)
- **sbt** (Scala Build Tool) — [install guide](https://www.scala-sbt.org/download.html)
- **Python 3** (only needed for the browsable prototype server, not for tests)

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
- **Working branching** — boolean and radio button pages route you down different paths based on your selection, exactly as defined in the journey JSON
- **Back links** — every page after the first has a "Back" link
- **Page navigator** — a debug strip at the bottom of every page with direct links (current page highlighted)
- **Custom port** — pass a port number as an argument: `python3 prototype/server.py 8080`

Try both journey paths:
1. Answer **Yes** to "Do you have a driver's license?" → vehicle type → features → rate experience
2. Answer **No** → preferred transportation method → thank you

## Journey Builder UI

The **Journey Builder** is a drag-and-drop visual editor for creating and editing
journey JSON files. It enables non-technical users — interaction designers, policy
owners, BA's — to define journeys without writing JSON by hand.

### Quick Start

```bash
cd journey-builder
npm install
npm run dev
```

Then open **http://localhost:5173** in your browser.

### Features

- **Drag-and-drop GDS components** — drag any of the 7 page types from the sidebar
  onto the canvas to add pages to your journey
- **Visual flow graph** — pages appear as styled nodes; draw connections between them
  to define the navigation order
- **Branching logic** — boolean (Yes/No) and radio button pages expose separate output
  handles for each option, letting you wire different paths visually
- **GDS-styled previews** — each node renders a mini-preview showing the form elements
  (text inputs, date fields, radios, checkboxes, question labels) so you can see at a
  glance what each page contains
- **Properties panel** — click any page to edit its title, options, questions, and
  validation regex in a right-hand panel
- **Real-time validation** — warnings appear live for empty titles, missing connections,
  unreachable pages, and empty option lists
- **Export to JSON** — generates a `journey.json` file conforming to
  `schema/journey.schema.json`, validated with Ajv before download
- **Import from JSON** — load an existing `journey.json` to visualise and edit it;
  auto-layout positions nodes cleanly using dagre
- **Auto-layout** — one-click dagre-based graph layout
- **Minimap and controls** — pan, zoom, and navigate large journeys

### How it works

1. Drag GDS component types from the left sidebar onto the canvas
2. Click a node to edit its title and properties in the right panel
3. Drag from an output handle (bottom of a node) to an input handle (top of another
   node) to connect pages
4. For branching pages (Yes/No, Radio Buttons), each option has its own output handle —
   connect each to the appropriate next page
5. Click **Export JSON** in the header to download the journey file
6. Feed the exported `journey.json` into the validator:

```bash
sbt -Djourney.json=path/to/exported/journey.json test
```

### Technology

| Component | Technology | Why |
|-----------|-----------|-----|
| Framework | React 18 | Component model, large ecosystem |
| Flow editor | @xyflow/react (React Flow) | Purpose-built for node-based visual editors |
| Build | Vite | Fast dev server and optimised production builds |
| Schema validation | Ajv | JSON Schema validation on export |
| Auto-layout | dagre | Directed graph layout algorithm |
| Language | TypeScript | Type safety matching the journey schema |

---

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

The key value proposition: when something changes, the validator catches it
immediately and tells you exactly where to fix it.

The `DriftDetectionSpec` test suite demonstrates five scenarios:
- **Title mismatch** — changing a page title in the prototype
- **Missing form field** — removing a required `<input>` element
- **Missing radio option** — dropping one option from a radio button group
- **Missing service route** — incomplete service descriptor
- **Correct page** — control case confirming valid HTML passes

### Interactive Demo

Run the demo script to see drift detection in action end-to-end:

```bash
chmod +x scripts/demo.sh
./scripts/demo.sh
```

The demo walks through five steps:
1. **Shows the journey** — prints every page from the JSON with its type, title, and navigation
2. **Validates the prototype + service** — runs `JourneyContractSpec` (should pass)
3. **Introduces drift** — changes `"What is your name?"` to `"What's your name?"` in the prototype HTML
4. **Re-validates** — runs the same spec again (should fail with a colour-highlighted failure showing the exact file and line to fix)
5. **Reverts the change** — restores the original prototype file

The whole thing takes about 30 seconds and leaves the repo in a clean state.

---

## Project Structure

```
journey-validator/
├── build.sbt                              # SBT build definition + system property forwarding
├── project/
│   └── build.properties                   # SBT version (1.10.7)
│
├── schema/
│   └── journey.schema.json                # JSON Schema for journey definitions (7 page types)
│
├── example/
│   └── journey.json                       # Example journey (9-page survey, 3 unique paths)
│
├── prototype/
│   ├── server.py                          # Browsable prototype server (Python 3, port 4000)
│   ├── page-0-welcome.html                # GOV.UK-styled prototype pages
│   ├── page-1-name.html                   #   named page-{index}-{slug}.html
│   ├── page-2-dob.html
│   ├── page-3-license.html                # Boolean branching page
│   ├── page-4-vehicle.html                # Radio button branching page
│   ├── page-5-features.html               # Checkbox page
│   ├── page-6-transport.html              # Radio button page
│   ├── page-7-rate.html                   # Multiple questions page
│   └── page-8-thankyou.html               # Terminal content page
│
├── service/
│   └── routes.json                        # Service route descriptor (mirrors journey.json)
│
├── src/main/scala/contract/
│   ├── Types.scala                        # Page, Journey, PageIndex, SourceRef case classes
│   ├── JourneyParser.scala                # JSON → Journey model (play-json, polymorphic index)
│   ├── JourneyGraph.scala                 # DFS graph traversal + path enumeration
│   ├── PrototypeValidator.scala           # HTML validation + ValidationResult/Report types
│   ├── ServiceValidator.scala             # Service route validation
│   └── SourceLocator.scala                # File:line lookup for failure messages
│
├── src/test/scala/contract/
│   ├── ValidatorConfig.scala              # System-property-driven paths (journey, prototype, service)
│   ├── EmbeddedServer.scala               # Zero-dependency HTTP server for prototype pages
│   ├── JourneyContractSpec.scala          # Main JSON-driven validation suite (23 tests)
│   └── DriftDetectionSpec.scala           # Deliberate-drift demonstration suite (5 tests)
│
├── scripts/
│   ├── validate-journey.sh                # Configurable test runner (flags + env vars)
│   └── demo.sh                            # Interactive drift detection demo
│
├── journey-builder/                       # Drag-and-drop visual journey editor
│   ├── package.json                       # React + React Flow + Ajv + dagre
│   ├── vite.config.ts                     # Vite build configuration
│   ├── tsconfig.json                      # TypeScript configuration
│   ├── index.html                         # Entry point
│   └── src/
│       ├── main.tsx                       # App bootstrap
│       ├── App.tsx                        # Main layout, state management, import/export
│       ├── components/
│       │   ├── Canvas.tsx                 # React Flow canvas with drag-and-drop
│       │   ├── Sidebar.tsx                # Draggable GDS component palette
│       │   ├── PropertiesPanel.tsx        # Node property editor (title, options, validation)
│       │   └── nodes/                     # Custom React Flow node renderers (one per page type)
│       │       ├── nodeTypes.ts           # Node type registry and shared data interface
│       │       ├── ContentPageNode.tsx
│       │       ├── StringNode.tsx
│       │       ├── DatePageNode.tsx
│       │       ├── BooleanNode.tsx        # Branching: true/false output handles
│       │       ├── RadioButtonNode.tsx    # Branching: per-option output handles
│       │       ├── CheckboxNode.tsx
│       │       └── MultipleQuestionsNode.tsx
│       ├── hooks/
│       │   ├── useJourneyExport.ts       # Nodes + edges → journey.json conversion
│       │   └── useJourneyImport.ts       # journey.json → nodes + edges with auto-layout
│       ├── types/
│       │   └── journey.ts                # TypeScript types mirroring the JSON schema
│       ├── utils/
│       │   ├── schemaValidator.ts        # Ajv-based validation against journey.schema.json
│       │   └── autoLayout.ts             # dagre-based directed graph layout
│       └── styles/
│           └── gds-theme.css             # GOV.UK-inspired styling for the editor
│
├── BRIEF.md                               # Hackathon pitch / story
└── README.md                              # This file
```

## How to Add Your Own Journey

1. **Write the journey JSON** following `schema/journey.schema.json`. Each page needs
   a `type`, `title`, and `index`. Add `options` for radio/checkbox pages, `questions`
   for multipleQuestionsPage.

2. **Create prototype HTML files** in a directory, named `page-{index}-{slug}.html`.
   Each file must have an `<h1>` containing the page title, and the appropriate form
   elements for its type (text input, radio buttons, checkboxes, etc.).

3. **Create a service route descriptor** as a JSON file with a `routes` array. Each
   route needs `method`, `path`, `pageType`, `title`, and (for branching pages)
   `nextRoutes`.

4. **Run the validator** against your files — no code changes needed:

```bash
sbt \
  -Djourney.json=your/journey.json \
  -Dprototype.dir=your/prototype \
  -Dservice.json=your/routes.json \
  test
```

## Extending the Validator

### Adding a new page type

1. Add the type to `schema/journey.schema.json` as a new `oneOf` entry
2. Add a case to `validatePageElements` in `PrototypeValidator.scala` that checks the
   appropriate HTML elements for the new type
3. The parser, graph traversal, service validator, and test suite will handle the new
   type automatically (they are driven by the JSON, not by hardcoded types)

### Adding a new validation check

1. Add the check logic in the appropriate validator (`PrototypeValidator` or
   `ServiceValidator`)
2. Return `Pass(...)` or `Fail(...)` — attach `contractRef` and `sourceRef` using
   `SourceLocator` methods so failures point to the right files
3. The check will automatically appear in test output and the `ValidationReport`

### Adding a new source file type

If you need to validate something beyond HTML prototypes and service route
descriptors (e.g. a React component tree, a Cypress test file):

1. Create a new validator object following the pattern of `PrototypeValidator`
2. Add corresponding `SourceLocator` methods for line-number resolution
3. Add a new section to `JourneyContractSpec` (or a new spec) that calls your validator
4. The file path should be configurable via a system property in `ValidatorConfig`

## Technology Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Build | SBT 1.10.7 | Standard Scala build tool |
| Language | Scala 2.13 | Type-safe, expressive, Play-compatible |
| JSON parsing | play-json 3.0.4 | Native Play Framework JSON library |
| HTML parsing | Jsoup 1.18.1 | Fast, reliable HTML parser (no browser needed) |
| Testing | ScalaTest 3.2.19 | Standard Scala test framework, dynamic test generation |
| Test server | Java HttpServer | Zero-dependency embedded server for prototype pages |
| Prototype server | Python 3 | Browse the journey in a real browser with GOV.UK styling |

### Why these choices

- **Scala + play-json**: Government services on GOV.UK are typically built with Scala
  and the Play Framework. Using the same language and JSON library means the validator
  can be dropped into existing CI pipelines and shares the same dependency ecosystem.

- **Jsoup over Selenium**: Jsoup parses HTML statically — no browser, no WebDriver,
  no flaky timeouts. Tests run in ~1 second. Selenium could be added later for
  JavaScript-heavy prototypes, but for GOV.UK's progressive enhancement approach,
  static HTML parsing is sufficient and much faster.

- **ScalaTest FreeSpec**: The `FreeSpec` style allows dynamic test generation from
  the journey JSON. Tests are created at suite construction time by iterating over
  pages and paths, so adding pages to the journey automatically adds tests.

- **Embedded HttpServer**: Java's built-in HTTP server has zero dependencies and
  starts in milliseconds. The prototype files are read from disk on every request,
  so changes to HTML are picked up immediately without restarting.

## Future Scope

- **Selenium/Playwright browser tests** — for full browser-based validation including JavaScript interactions
- **Cross-framework support** — validate Node.js prototypes, React SPAs, or any HTML-based journey
- **Central validation API** — a microservice that any team can call to validate their journey
- **AI auto-fix** — detect drift and generate a PR to fix it automatically
- ~~**Visual journey builder** — drag-and-drop UI for creating journey JSONs~~ **Done** — see `journey-builder/`
- **CI pipeline integration** — fail the build when the contract is violated
- **Schema evolution** — versioned journey contracts with migration support
