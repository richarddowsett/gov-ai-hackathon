# AI Journey Contract Validator

## Overview

This project demonstrates how a **single JSON journey definition** can act as a **source of truth** to validate:

- Service design
- Prototype
- Implementation
- User journeys
- AI-generated code

The goal is to **prevent drift** between design → prototype → implementation, especially in an AI-driven development workflow.

---

# Problem

Designs, prototypes, and final implementations frequently diverge.

Examples:
- Prototype changes not implemented
- Implementation modifies journey
- AI generates incorrect flow
- Manual human errors

We need **confidence** that what we designed is what we shipped.

---

# Solution

Use a **JSON Journey Schema** as the single source of truth.

From this JSON we:

1. Validate prototype
2. Validate implementation
3. Generate journey tests
4. (Optional) Generate prototype
5. (Optional) Generate service
6. (Future) Auto-fix via AI

---

# Architecture

```
JSON Journey Schema
        │
        ├── Prototype Validator
        │
        ├── Service Validator
        │
        └── Journey Test Runner
                │
        Contract Validation Output
```

---

# Example JSON

```json
{
  "journey": "user-registration",
  "pages": [
    {
      "id": "start",
      "title": "Enter your name",
      "type": "form",
      "fields": [
        {
          "name": "fullName",
          "type": "text",
          "validation": "^[A-Za-z ]+$"
        }
      ],
      "next": "email"
    },
    {
      "id": "email",
      "title": "Enter email",
      "type": "form",
      "fields": [
        {
          "name": "email",
          "type": "email"
        }
      ],
      "next": "confirm"
    }
  ]
}
```

---

# MVP Scope

## Must Have

- JSON schema
- Example journey
- Prototype validator
- Service validator
- Contract test
- Drift detection

## Nice To Have

- Generate prototype from JSON
- Generate tests from JSON
- Diff output
- CLI

## Stretch Goals

- Central validation API
- Multi-framework validation
- AI auto-fix PR
- Visual journey builder

---

# Demo Story

> Define journey once in JSON  
> Validate prototype  
> Validate implementation  
> Detect drift  
> Auto-fix using AI

---

# Team Breakdown (5 People)

---

# Person 1 — JSON Schema Architect

### Responsibilities

- Define JSON schema
- Define page types
- Define transitions
- Define validation structure
- Create example journey

### Output

```
journey.schema.json
example-journey.json
```

---

# Person 2 — Prototype Validator

Validates HTML prototype against JSON.

### Responsibilities

- Parse JSON
- Traverse prototype pages
- Extract page titles
- Extract fields
- Compare with schema
- Output diff

### Output

```
prototype-validator.ts
```

---

# Person 3 — Service Validator

Validates real implementation (Play / Scala).

### Responsibilities

- Load JSON
- Run journey test
- Capture pages visited
- Validate transitions
- Validate content

### Output

```
JourneyContractSpec.scala
```

---

# Person 4 — Journey Test Runner

Reusable test execution engine.

### Responsibilities

- Execute journey
- Capture navigation
- Store state
- Output visited graph
- Feed validator

### Output

```
journey-runner
journey-state.json
```

---

# Person 5 — Demo + Integration

Glue everything together.

### Responsibilities

- CLI tool
- Demo script
- Diff output
- Presentation flow
- Optional AI fix suggestion

### Output

```
validate-journey
demo.sh
```

---

# Workflow

## Step 1

Create JSON

```
journey.json
```

## Step 2

Build prototype

```
prototype/
```

## Step 3

Validate prototype

```
validate prototype journey.json
```

## Step 4

Build service

```
service/
```

## Step 5

Validate service

```
validate service journey.json
```

---

# Drift Detection Example

Change:

```
Enter your name
```

To:

```
What's your name?
```

Result:

```
❌ Journey mismatch

Page: start
Expected: Enter your name
Actual: What's your name?
```

---

# Suggested Tech Stack

Prototype
- HTML
- GOV.UK template

Service
- Scala Play

Validation
- Node.js
- TypeScript (optional)

Testing
- Playwright
- Gatling
- ScalaTest

---

# AI Usage Strategy

Use AI for:

- Schema generation
- Validator generation
- Test generation
- Diff engine
- CLI generation
- Demo setup

Do not use AI for:

- Architecture decisions
- Schema ownership
- Demo story

---

# Execution Plan (4 Hours)

Hour 1  
Schema + example journey

Hour 2  
Prototype validator

Hour 3  
Service validator

Hour 4  
Demo + integration

---

# Demo Flow

1. Show JSON
2. Show prototype
3. Run validator
4. Break prototype
5. Show failure
6. Fix issue
7. Show pass
8. Explain future AI generation

---

# Future Vision

This becomes:

- Cross-framework validator
- Government-wide journey contracts
- AI-generated services
- Automated PR fixes
- Visual journey builder
- CI pipeline validator

---

# Repository Structure

```
.
├── schema/
│   └── journey.schema.json
│
├── example/
│   └── journey.json
│
├── prototype/
│
├── service/
│
├── validator/
│   ├── prototype-validator
│   └── service-validator
│
├── runner/
│   └── journey-runner
│
└── README.md
```

---

# End Goal

Confidence in AI-generated development:

**Design once  
Validate everywhere  
Prevent drift  
Ship faster**