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
interaction designer and the developer. From this JSON, we generate contract tests
that validate:

1. **Prototype pages** — does the HTML prototype match the journey contract?
2. **Service implementation** — does the Scala/Play service implement the correct
   pages, titles, types, and branching logic?
3. **Drift detection** — if someone (or an AI agent) changes something, the validator
   catches it immediately with a clear diff.

## How It Works

```
Journey JSON (single source of truth)
        │
        ├── Parse into Scala model (7 page types, branching graph)
        │
        ├── Enumerate all valid paths through the journey
        │
        ├── Validate prototype HTML against each page's contract
        │       ✓ title matches    ✓ form fields exist    ✓ options present
        │
        └── Validate service descriptor against each page's contract
                ✓ route exists    ✓ page type matches    ✓ branching routes defined
```

## The Story

> "In this world of rapid AI-driven development, how do you guarantee that what you
> designed is what you shipped?"
>
> With the Journey Contract Validator, you define the journey once in JSON, and the
> contract tests guarantee that the prototype and the implementation match — every
> page title, every form field, every branching path.
>
> Change something? The tests fail. Immediately. With a clear message telling you
> exactly what drifted.

## Running

```bash
sbt test                                   # run everything
./scripts/validate-journey.sh prototype    # validate prototype only
./scripts/validate-journey.sh service      # validate service only
./scripts/validate-journey.sh drift        # run drift detection tests
./scripts/demo.sh                          # full demo walkthrough
```

## Future Vision

- Cross-framework validation (Play, Node.js, React)
- Central validation API for government-wide journey contracts
- AI-generated PR fixes when drift is detected
- Visual journey builder UI
- CI pipeline integration
