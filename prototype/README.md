# Prototype — GOV.UK HTML Journey

A set of static GOV.UK-styled HTML pages representing a 9-page survey journey,
served by a lightweight Python HTTP server with working form submissions, branching
logic, and back navigation — all driven by `example/journey.json`.

## Running

```bash
# From the repository root
python3 prototype/server.py
```

Then open **http://localhost:4000** in your browser.

To use a custom port:

```bash
python3 prototype/server.py 8080
```

Or run via Docker Compose (starts the prototype alongside the full stack):

```bash
./scripts/docker-up.sh
# Prototype at http://localhost:4000
```

## How It Works

The Python server (`server.py`) reads `example/journey.json` at startup and maps
each HTML page file to a URL based on its index and slug. When a form is submitted:

1. The server reads the form data
2. For branching pages (boolean, radioButton), it looks up the user's answer in the
   journey JSON's `index` map to determine the next page
3. For linear pages, it follows the fixed `index` to the next page
4. The browser is redirected (303) to the next page's URL

No JavaScript is needed — the server handles all navigation server-side, exactly
as the real GOV.UK Prototype Kit would.

## Features

- **GOV.UK styling** — header, phase banner, green action buttons, form elements
  matching the GOV.UK Design System
- **Working branching** — boolean and radio button pages route to different paths
  based on your selection, exactly as defined in `journey.json`
- **Back links** — every page after the first has a "Back" link
- **Page navigator** — a debug strip at the bottom of every page with direct links
  to all pages (current page highlighted)
- **Zero dependencies** — uses only Python standard library modules

## Journey Paths

Try all three paths through the survey:

1. **Yes → Car/Motorcycle/Truck**: License (yes) → Vehicle type → Features → Rate experience
2. **Yes → Other**: License (yes) → Vehicle type (Other) → Transport method → Thank you
3. **No**: License (no) → Transport method → Thank you

## Pages

| File | Type | Title |
|------|------|-------|
| `page-0-welcome.html` | contentPage | Welcome to our survey |
| `page-1-name.html` | string | What is your name? |
| `page-2-dob.html` | datePage | What is your date of birth? |
| `page-3-license.html` | boolean | Do you have a driver's license? |
| `page-4-vehicle.html` | radioButton | What type of vehicle do you own? |
| `page-5-features.html` | checkbox | Select all vehicle features you have: |
| `page-6-transport.html` | radioButton | What is your preferred transportation method? |
| `page-7-rate.html` | multipleQuestionsPage | Rate your experience |
| `page-8-thankyou.html` | contentPage | Thank you for completing the survey! |

### Naming convention

Pages follow the pattern `page-{index}-{slug}.html` where `{index}` is the
zero-based position in the journey JSON array and `{slug}` is a human-readable
name. The server and validation libraries match files by index.

## Validation

The prototype is validated against `journey.json` using the
[prototype-validation](../prototype-validation/) Python library and pytest:

```bash
cd prototype
python3 -m venv .venv
source .venv/bin/activate
pip install -r tests/requirements.txt
python3 -m pytest tests/ -v
```

This runs **16 tests**:

- **4 structure tests** — journey loads, titles non-empty, types recognised, paths
  enumerable
- **9 page validation tests** — every page has the correct `<h1>` title and form
  elements matching the journey contract
- **3 path validation tests** — every unique path is walked, validating each page
  along the way

## Project Structure

```
prototype/
├── server.py                  # Python HTTP server (GOV.UK styling, navigation)
├── page-0-welcome.html        # Static HTML pages (GOV.UK Design System)
├── page-1-name.html
├── page-2-dob.html
├── page-3-license.html
├── page-4-vehicle.html
├── page-5-features.html
├── page-6-transport.html
├── page-7-rate.html
├── page-8-thankyou.html
├── tests/
│   ├── conftest.py            # pytest config (loads prototype-validation)
│   ├── test_journey_validation.py  # Test suite (page + path validation)
│   └── requirements.txt       # Python dependencies (beautifulsoup4, pytest)
└── README.md                  # This file
```

## Adding Pages

To add a page to the prototype:

1. Add the page definition to `example/journey.json`
2. Create `page-{index}-{slug}.html` following the GOV.UK markup pattern of the
   existing pages — an `<h1>` with the page title and the appropriate form elements
   for the page type
3. Run the validation tests — any missing or incorrect elements will be flagged
