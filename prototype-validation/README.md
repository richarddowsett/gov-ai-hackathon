# Prototype Validation — Python Journey Validation Library

A Python library for validating GOV.UK HTML prototype pages against a
`journey.json` contract. The Python counterpart to the Scala
`journey-validation` library.

## What It Does

Given a `journey.json` (the single source of truth) and a directory of
prototype HTML files, the library automatically:

1. **Parses** the JSON into a typed Python model
2. **Builds a graph** and enumerates every valid path through the journey
3. **Validates each HTML page** — checks `<h1>` titles, form elements
   (`<input type="text">`, radios, checkboxes, date fields), option labels,
   and question labels against the journey contract
4. **Reports failures** with clear expected-vs-actual messages

## Installation

```bash
pip install -e prototype-validation/
```

Or add the path to your test configuration (see the prototype test suite for
an example using `sys.path`).

## Quick Start

```python
from journeyvalidation import PrototypeTestSuite

suite = PrototypeTestSuite("example/journey.json", "prototype")

# Validate all pages at once
report = suite.validate_all_pages()
print(report.render())
assert report.all_passed

# Validate a single page
results = suite.validate_page(0)

# Walk every unique path
for path in suite.unique_paths:
    results = suite.validate_path(path)
```

## Using with pytest

The prototype ships with a test suite at `prototype/tests/`. Run it with:

```bash
pip install beautifulsoup4 pytest
pytest prototype/tests/ -v
```

The test file auto-generates parametrised tests — one per page and one per
unique journey path:

```
TestJourneyStructure::test_journey_loads_with_pages PASSED
TestJourneyStructure::test_all_titles_non_empty PASSED
TestJourneyStructure::test_all_types_recognised PASSED
TestJourneyStructure::test_has_traversable_paths PASSED
TestPageValidation::test_page[page[0] (contentPage) Welcome to our survey] PASSED
TestPageValidation::test_page[page[1] (string) What is your name?] PASSED
  ... (all 9 pages)
TestPathValidation::test_path[path 1: ...] PASSED
TestPathValidation::test_path[path 2: ...] PASSED
TestPathValidation::test_path[path 3: ...] PASSED
```

## Library API

### `PrototypeTestSuite(journey_path, prototype_dir)`

High-level test helper. Reads the journey JSON and provides methods to
validate prototype pages.

| Method | Returns | Description |
|--------|---------|-------------|
| `validate_page(index)` | `list[ValidationResult]` | Validate a single page by array index |
| `validate_all_pages()` | `ValidationReport` | Validate every page in the journey |
| `validate_path(path)` | `list[ValidationResult]` | Validate every page in a journey path |
| `validate_all_paths()` | `ValidationReport` | Validate every unique path |
| `unique_paths` | `list[JourneyPath]` | All unique paths through the journey |

### Lower-level functions

| Function | Module | Description |
|----------|--------|-------------|
| `parse(json_string)` | `parser` | Parse a JSON string into a `Journey` |
| `parse_file(path)` | `parser` | Parse a JSON file into a `Journey` |
| `validate_html(html, page, label)` | `page_validator` | Validate an HTML string against a `Page` |
| `enumerate_all_paths(journey)` | `graph` | All paths (including duplicates from radio fan-out) |
| `unique_paths(journey)` | `graph` | Deduplicated paths |

## Project Structure

```
prototype-validation/
├── pyproject.toml
├── README.md
└── journeyvalidation/
    ├── __init__.py
    ├── model.py              # Journey, Page, Pass, Fail, ValidationReport
    ├── parser.py             # JSON → Journey model
    ├── graph.py              # DFS path enumeration
    ├── page_validator.py     # HTML validation with BeautifulSoup
    └── prototype_suite.py    # High-level test helper
```
