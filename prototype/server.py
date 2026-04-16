#!/usr/bin/env python3
"""
Lightweight prototype server for the Journey Contract Validator.

Serves the GOV.UK-styled HTML prototype pages with working navigation,
form submissions, and branching logic driven by the journey JSON.

Usage:
    python3 prototype/server.py
    # then open http://localhost:4000 in your browser

    python3 prototype/server.py 8080
    # use a custom port
"""

import json
import os
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import parse_qs, urlparse

PROTOTYPE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(PROTOTYPE_DIR)
JOURNEY_FILE = os.path.join(PROJECT_DIR, "example", "journey.json")

with open(JOURNEY_FILE) as f:
    JOURNEY = json.load(f)

PAGES = JOURNEY["pages"]

PAGE_FILES = {}
for fname in os.listdir(PROTOTYPE_DIR):
    if fname.startswith("page-") and fname.endswith(".html"):
        idx = int(fname.split("-")[1])
        PAGE_FILES[idx] = os.path.join(PROTOTYPE_DIR, fname)

ROUTE_MAP = {}
for idx, fname in PAGE_FILES.items():
    slug = os.path.basename(fname).replace(".html", "")
    ROUTE_MAP[f"/{slug}"] = idx
ROUTE_MAP["/"] = 0

SESSION = {}


def resolve_next_page(page_index, answer=None):
    """Given a page index and optional answer, return the next page index."""
    if page_index >= len(PAGES):
        return None
    page = PAGES[page_index]
    idx = page["index"]

    if isinstance(idx, dict):
        if answer and answer in idx:
            return idx[answer]
        return list(idx.values())[0]
    else:
        return idx if idx < len(PAGES) else None


def slug_for_index(page_index):
    if page_index is None or page_index >= len(PAGES):
        return None
    fname = PAGE_FILES.get(page_index)
    if fname:
        return os.path.basename(fname).replace(".html", "")
    return None


GOVUK_CSS = """
<style>
  * { box-sizing: border-box; }
  body {
    font-family: "GDS Transport", arial, sans-serif;
    font-size: 19px; line-height: 1.5;
    color: #0b0c0c; background: #fff;
    margin: 0; padding: 0;
  }
  .govuk-header {
    background: #0b0c0c; color: #fff;
    padding: 10px 0; border-bottom: 10px solid #1d70b8;
  }
  .govuk-header__container {
    max-width: 960px; margin: 0 auto; padding: 0 15px;
  }
  .govuk-header__link { color: #fff; text-decoration: none; font-size: 24px; font-weight: 700; }
  .govuk-phase-banner {
    border-bottom: 1px solid #b1b4b6; padding: 10px 0;
    max-width: 960px; margin: 0 auto; padding: 10px 15px;
  }
  .govuk-tag {
    background: #1d70b8; color: #fff; padding: 2px 8px;
    font-size: 14px; font-weight: 700; text-transform: uppercase;
    letter-spacing: 1px;
  }
  .govuk-width-container { max-width: 960px; margin: 0 auto; padding: 0 15px; }
  .govuk-main-wrapper { padding: 40px 0; }
  .govuk-heading-xl { font-size: 48px; font-weight: 700; margin: 0 0 30px; line-height: 1.1; }
  .govuk-heading-l { font-size: 36px; font-weight: 700; margin: 0 0 20px; line-height: 1.1; }
  .govuk-body { margin: 0 0 20px; }
  .govuk-label { display: block; margin-bottom: 5px; font-weight: 700; }
  .govuk-label--m { font-size: 24px; }
  .govuk-label--xl { font-size: 48px; line-height: 1.1; margin-bottom: 15px; }
  .govuk-input {
    font-size: 19px; padding: 5px; border: 2px solid #0b0c0c;
    width: 100%; max-width: 500px; height: 40px;
  }
  .govuk-input--width-2 { width: 5.4ex; }
  .govuk-input--width-4 { width: 9ex; }
  .govuk-button {
    background: #00703c; color: #fff; border: none;
    padding: 8px 20px; font-size: 19px; font-weight: 700;
    cursor: pointer; text-decoration: none;
    display: inline-block; margin-top: 10px;
    box-shadow: 0 2px 0 #002d18;
  }
  .govuk-button:hover { background: #005a30; }
  .govuk-button--start {
    background: #00703c; font-size: 24px; padding: 10px 24px;
  }
  .govuk-radios__item, .govuk-checkboxes__item {
    display: flex; align-items: center; margin-bottom: 10px;
  }
  .govuk-radios__input, .govuk-checkboxes__input {
    width: 44px; height: 44px; margin-right: 12px; cursor: pointer;
  }
  .govuk-radios__label, .govuk-checkboxes__label {
    font-size: 19px; cursor: pointer;
  }
  .govuk-fieldset__heading { font-size: 48px; font-weight: 700; margin: 0 0 30px; line-height: 1.1; }
  .govuk-fieldset__legend--xl { padding: 0; margin: 0; }
  .govuk-form-group { margin-bottom: 30px; }
  .govuk-date-input { display: flex; gap: 20px; }
  .govuk-date-input__item { display: flex; flex-direction: column; }
  .govuk-date-input__label { font-weight: 400; margin-bottom: 5px; }
  .govuk-panel {
    background: #00703c; color: #fff; padding: 40px;
    text-align: center; margin-bottom: 30px;
  }
  .govuk-panel__title { font-size: 48px; font-weight: 700; margin: 0 0 20px; }
  .govuk-panel__body { font-size: 36px; }
  .govuk-back-link {
    color: #0b0c0c; font-size: 16px; text-decoration: underline;
    display: inline-block; margin-bottom: 15px;
  }
  fieldset { border: none; padding: 0; margin: 0; }
  .journey-nav {
    margin-top: 40px; padding-top: 20px;
    border-top: 1px solid #b1b4b6; font-size: 14px; color: #6f7781;
  }
  .journey-nav a { color: #1d70b8; }
</style>
"""

HEADER_HTML = """
<header class="govuk-header" role="banner">
  <div class="govuk-header__container">
    <a class="govuk-header__link" href="/">GOV.UK</a>
  </div>
</header>
<div class="govuk-phase-banner">
  <span class="govuk-tag">prototype</span>
  &nbsp; This is a prototype — your answers will not be saved.
</div>
"""

def build_nav_footer(page_index):
    """Build a small debug footer showing all pages for easy navigation."""
    links = []
    for i in range(len(PAGES)):
        p = PAGES[i]
        slug = slug_for_index(i)
        label = f"[{i}] {p['title'][:30]}"
        if i == page_index:
            links.append(f"<strong>{label}</strong>")
        else:
            links.append(f'<a href="/{slug}">{label}</a>')
    return f'<div class="journey-nav">Pages: {" | ".join(links)}</div>'


def inject_styling(html, page_index):
    """Inject GOV.UK CSS, header, and navigation footer into an HTML page."""
    html = html.replace("</head>", GOVUK_CSS + "\n</head>")
    html = html.replace('<body class="govuk-template__body">', f'<body class="govuk-template__body">\n{HEADER_HTML}')
    nav = build_nav_footer(page_index)
    html = html.replace("</main>", f"{nav}\n</main>")
    return html


def rewrite_form_action(html, page_index):
    """Rewrite form actions to POST to the current page (server handles redirect)."""
    slug = slug_for_index(page_index)
    import re
    html = re.sub(r'action="[^"]*"', f'action="/{slug}" method="post"', html)

    if page_index > 0:
        prev_slug = slug_for_index(page_index - 1)
        if prev_slug:
            back_link = f'<a class="govuk-back-link" href="/{prev_slug}">Back</a>'
            html = html.replace('<main class="govuk-main-wrapper" role="main">',
                                f'<main class="govuk-main-wrapper" role="main">\n      {back_link}')
    return html


class PrototypeHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"

        if path in ROUTE_MAP:
            page_index = ROUTE_MAP[path]
            self.serve_page(page_index)
        elif path == "/complete":
            self.send_response(200)
            self.send_header("Content-Type", "text/html")
            self.end_headers()
            html = f"""<!DOCTYPE html>
<html lang="en" class="govuk-template"><head><meta charset="utf-8">
<title>Journey complete - GOV.UK</title>{GOVUK_CSS}</head>
<body class="govuk-template__body">{HEADER_HTML}
<div class="govuk-width-container"><main class="govuk-main-wrapper" role="main">
<div class="govuk-panel"><h1 class="govuk-panel__title">Journey complete</h1>
<div class="govuk-panel__body">You have reached the end of this path.</div></div>
<p class="govuk-body"><a href="/" class="govuk-button">Start again</a></p>
{build_nav_footer(-1)}</main></div></body></html>"""
            self.wfile.write(html.encode())
        else:
            self.send_error(404, f"Page not found: {path}")

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length).decode() if content_length else ""
        form_data = parse_qs(body)
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/")

        page_index = ROUTE_MAP.get(path)
        if page_index is None:
            self.send_error(404)
            return

        page = PAGES[page_index]
        answer = None
        idx = page["index"]

        if isinstance(idx, dict):
            for field_name, values in form_data.items():
                value = values[0] if values else None
                if value and value in idx:
                    answer = value
                    break

        next_index = resolve_next_page(page_index, answer)
        next_slug = slug_for_index(next_index)

        if next_slug:
            redirect_to = f"/{next_slug}"
        else:
            redirect_to = "/complete"

        self.send_response(303)
        self.send_header("Location", redirect_to)
        self.end_headers()

    def serve_page(self, page_index):
        fpath = PAGE_FILES.get(page_index)
        if not fpath or not os.path.exists(fpath):
            self.send_error(404)
            return

        with open(fpath) as f:
            html = f.read()

        html = inject_styling(html, page_index)
        html = rewrite_form_action(html, page_index)

        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=UTF-8")
        self.end_headers()
        self.wfile.write(html.encode())

    def log_message(self, format, *args):
        method_path = args[0] if args else ""
        status = args[1] if len(args) > 1 else ""
        print(f"  {method_path}  {status}")


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 4000
    host = sys.argv[2] if len(sys.argv) > 2 else "localhost"
    server = HTTPServer((host, port), PrototypeHandler)
    print(f"""
  ╔═══════════════════════════════════════════════════╗
  ║   GOV.UK Journey Prototype Server                 ║
  ║                                                   ║
  ║   Running at: http://{host}:{port:<5}                  ║
  ║   Journey:    example/journey.json                ║
  ║   Pages:      {len(PAGE_FILES)} prototype pages                  ║
  ║                                                   ║
  ║   Press Ctrl+C to stop                            ║
  ╚═══════════════════════════════════════════════════╝
""")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n  Server stopped.")
        server.server_close()


if __name__ == "__main__":
    main()
