# Example Service — Play Framework Journey Implementation

A full Play 3.0 Framework application implementing a 9-page GOV.UK-styled survey,
driven entirely by a `journey.json` definition. Validated end-to-end by the
`journey-validation` library.

## How It Works

The service reads `conf/journey.json` at startup and uses it to:

1. **Render pages** — A single `JourneyController` serves all pages. The Twirl
   template (`page.scala.html`) pattern-matches on the page type and renders the
   appropriate GOV.UK form elements (text inputs, date fields, radios, checkboxes,
   multi-question forms, or content-only pages).

2. **Handle navigation** — Form submissions are routed to the next page based on the
   journey graph. Linear pages advance to a fixed next index. Branching pages
   (boolean, radioButton) look up the user's answer in the `index` map to determine
   the target page.

3. **No hardcoded pages** — Adding or removing pages from `journey.json` changes the
   service with zero code modifications. The controller, templates, and tests all
   derive their behaviour from the JSON.

## Running

```bash
# From the repository root
sbt "exampleService/run"
```

Then open **http://localhost:9000** in your browser.

## Routes

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Redirects to `/page/0` |
| GET | `/page/:index` | Renders the page at the given array index |
| POST | `/page/:index` | Processes form submission, redirects to next page |
| GET | `/complete` | Journey complete confirmation |

## Testing

The `JourneyValidationSpec` uses the `journey-validation` library's `JourneySpec`
trait to automatically validate the service against `journey.json`:

```bash
sbt "exampleService/test"
```

This runs **16 tests**:

- **4 structure tests** — journey loads, titles non-empty, types recognised, paths
  enumerable
- **9 page validation tests** — every page renders with the correct `<h1>` title and
  form elements matching the journey contract
- **3 path validation tests** — every unique journey path is walked end-to-end,
  validating page HTML and verifying that form submissions redirect to the correct
  next page

### How the tests work

The spec mixes in `JourneySpec` and `GuiceOneAppPerSuite`, which boots the Play
application in-process. No external server is needed. For each test:

1. `route(app, FakeRequest(GET, "/page/{index}"))` renders the page
2. `PageValidator` parses the HTML with Jsoup and checks it against the journey
   contract (title, form elements, option labels)
3. For path tests, `route(app, FakeRequest(POST, "/page/{index}"))` submits the form
   and `redirectLocation` is verified against the expected next page

## Project Structure

```
example-service/
├── app/
│   ├── controllers/
│   │   └── JourneyController.scala    # Reads journey.json, renders pages, handles nav
│   └── views/
│       ├── main.scala.html            # GOV.UK layout (header, styling)
│       ├── page.scala.html            # Dynamic page template (all 7 page types)
│       └── complete.scala.html        # Journey complete confirmation
├── conf/
│   ├── application.conf               # Play configuration
│   ├── routes                         # URL routing
│   └── journey.json                   # Journey definition (source of truth)
└── test/scala/
    └── JourneyValidationSpec.scala    # Journey validation using the library
```

## Supported Page Types

The `page.scala.html` template handles all 7 page types defined in the journey schema:

| Type | Rendered elements |
|------|-------------------|
| `contentPage` | `<h1>` + Continue button |
| `string` | `<h1>` + `<input type="text">` |
| `datePage` | `<h1>` + Day/Month/Year text inputs |
| `boolean` | `<h1>` + Yes/No radio buttons |
| `radioButton` | `<h1>` + radio button per option from journey.json |
| `checkbox` | `<h1>` + checkbox per option from journey.json |
| `multipleQuestionsPage` | `<h1>` + labelled text input per question |

## Adding to Your Own Service

This example service demonstrates the pattern. To validate your own Play service:

1. Add `journey-validation` as a dependency
2. Place your `journey.json` in `conf/`
3. Create a test spec:

```scala
class MyJourneySpec extends JourneySpec with GuiceOneAppPerSuite {
  override def fakeApplication() = new GuiceApplicationBuilder().build()
  lazy val journeyJson = {
    val s = getClass.getClassLoader.getResourceAsStream("journey.json")
    Using(Source.fromInputStream(s))(_.mkString).get
  }

  "Page validation" should { validatePages() }
  "Journey paths" should { validatePaths() }
}
```
