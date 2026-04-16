# AI Journey Contract Validator — Hackathon Brief

## Problem

In government digital services, designs, prototypes, and implementations frequently
diverge. An interaction designer agrees a journey with the team. A prototype is built.
A service is developed. But subtle changes creep in — a page title is changed, a form
field is removed, a branching path is altered. Human error is the number one cause.

In a world where AI agents are generating code at speed, **how do you know what they
built is correct?**

## Solution

A **journey JSON definition** acts as the single source of truth — created by the
interaction designer using a browser-based tool, stored centrally via a REST API, and
validated automatically against both the prototype and the production service.

### The platform

1. **JSON Creator** — browser UI for interaction designers to build journey definitions
   against a schema, and save them to the storage API
2. **Journey Storage** — a Play Framework microservice backed by PostgreSQL that
   stores journey JSON by service name, accessible via REST
3. **journey-validation** (Scala library) — validates a running Play Framework service
   against the journey contract: page titles, form fields, options, and navigation
4. **prototype-validation** (Python library) — validates static HTML prototype pages
   against the same contract using BeautifulSoup4
5. **Docker Compose** — runs the full stack (creator, storage, prototype, database)
   with one command

### What gets validated

- **Page rendering** — does each page have the right title, form fields, and options?
- **Navigation** — do form submissions route to the correct next page for every
  branching answer?
- **Full path coverage** — is every possible journey through the service valid
  end-to-end?

## How It Works

```
Interaction designer
        │
        ▼
JSON Creator (browser UI)
        │
        ▼
Journey Storage API (Play + PostgreSQL)
        │
   ┌────┴────┐
   ▼         ▼
Prototype   Play Service
   │         │
   ▼         ▼
prototype-  journey-
validation  validation
(Python)    (Scala)
   │         │
   ▼         ▼
pytest      sbt test
```

## The Story

> "In this world of rapid AI-driven development, how do you guarantee that what you
> designed is what you shipped?"
>
> With the Journey Contract Validator, the designer creates a journey in the browser,
> stores it centrally, and both the prototype and the service validate themselves
> against that contract. Change something? The tests fail. Immediately. With a clear
> message telling you exactly what drifted.

## Running

```bash
# Start the full stack (JSON Creator + Storage API + Prototype + PostgreSQL)
./scripts/docker-up.sh

# Validate the Play service against journey.json
sbt "exampleService/test"

# Validate the HTML prototype against journey.json
cd prototype && python3 -m pytest tests/ -v

# Browse the Play service
sbt "exampleService/run"             # http://localhost:9000/start

# Browse the HTML prototype
python3 prototype/server.py          # http://localhost:4000

# Interactive drift detection demo
./scripts/demo.sh
```

## Future Vision

- Fetch journey JSON from the storage API at test time
- Published libraries on Maven Central and PyPI
- Cross-framework validation (Play, Node.js, React)
- Central validation API for government-wide journey contracts
- AI-generated PR fixes when drift is detected
- CI pipeline integration
