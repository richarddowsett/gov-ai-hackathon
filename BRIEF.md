# AI Journey Contract Validator — Hackathon Brief

## Problem

In government digital services, designs, prototypes, and implementations frequently
diverge. An interaction designer agrees a journey with the team. A prototype is built.
A service is developed. But subtle changes creep in — a page title is changed, a form
field is removed, a branching path is altered. Human error is the number one cause.

In a world where AI agents are generating code at speed, **how do you know what they
built is correct?**

## Solution

A single **JSON journey definition** acts as the **source of truth** — agreed by the
interaction designer and the developer. From this JSON, a reusable **journey-validation
library** generates contract tests that validate a running Play Framework service:

1. **Page rendering** — does each page have the right title, form fields, and options?
2. **Navigation** — do form submissions route to the correct next page for every
   branching answer?
3. **Full path coverage** — is every possible journey through the service valid
   end-to-end?

## How It Works

```
Journey JSON (single source of truth)
        │
        ├── Parse into Scala model (7 page types, branching graph)
        │
        ├── Enumerate all valid paths through the journey
        │
        └── Validate the Play service against the contract
                ✓ page titles match      ✓ form elements exist
                ✓ options present        ✓ navigation correct
                ✓ all paths traversable
```

## The Story

> "In this world of rapid AI-driven development, how do you guarantee that what you
> designed is what you shipped?"
>
> With the Journey Contract Validator, you define the journey once in JSON, add the
> `journey-validation` library to your Play service, and get full end-to-end journey
> testing for free — every page title, every form field, every branching path, every
> navigation redirect.
>
> Change something? The tests fail. Immediately. With a clear message telling you
> exactly what drifted.

## Running

```bash
sbt test                          # run all tests (library + Play service)
sbt "exampleService/test"         # validate the example Play service
sbt "exampleService/run"          # browse the service at localhost:9000
./scripts/validate-journey.sh     # run via helper script
./scripts/demo.sh                 # interactive drift detection demo
python3 prototype/server.py       # browse the HTML prototype at localhost:4000
```

## Future Vision

- Published library on Maven Central for any Play service to depend on
- Cross-framework validation (Play, Node.js, React)
- Central validation API for government-wide journey contracts
- AI-generated PR fixes when drift is detected
- Visual journey builder UI
- CI pipeline integration
