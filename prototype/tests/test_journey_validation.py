"""Validate the prototype HTML pages against the journey JSON contract.

Uses the journey-validation Python library to automatically verify that
every page has the correct title, form elements, and option labels, and
that every unique path through the journey can be traversed.
"""

import os
import sys

import pytest

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
sys.path.insert(0, os.path.join(PROJECT_ROOT, "prototype-validation"))

from journeyvalidation import (
    PrototypeTestSuite,
    ValidationReport,
)

JOURNEY_PATH = os.path.join(PROJECT_ROOT, "example", "journey.json")
PROTOTYPE_DIR = os.path.join(PROJECT_ROOT, "prototype")

suite = PrototypeTestSuite(JOURNEY_PATH, PROTOTYPE_DIR)


# ---------------------------------------------------------------------------
# Journey structure
# ---------------------------------------------------------------------------

class TestJourneyStructure:

    def test_journey_loads_with_pages(self):
        assert suite.journey.page_count > 0

    def test_all_titles_non_empty(self):
        for page in suite.journey.pages:
            assert page.title, f"Page '{page.page_type}' has empty title"

    def test_all_types_recognised(self):
        known = {
            "contentPage", "string", "datePage", "boolean",
            "radioButton", "checkbox", "multipleQuestionsPage",
        }
        for page in suite.journey.pages:
            assert page.page_type in known, f"Unknown type '{page.page_type}'"

    def test_has_traversable_paths(self):
        assert len(suite.unique_paths) > 0


# ---------------------------------------------------------------------------
# Per-page HTML validation
# ---------------------------------------------------------------------------

class TestPageValidation:

    @pytest.mark.parametrize(
        "idx,page",
        list(enumerate(suite.journey.pages)),
        ids=[
            f"page[{i}] ({p.page_type}) {p.title}"
            for i, p in enumerate(suite.journey.pages)
        ],
    )
    def test_page(self, idx, page):
        results = suite.validate_page(idx)
        report = ValidationReport(f"page[{idx}]", results)
        assert report.all_passed, f"\n{report.render()}"


# ---------------------------------------------------------------------------
# Per-path validation (walks every unique path through the journey)
# ---------------------------------------------------------------------------

class TestPathValidation:

    @pytest.mark.parametrize(
        "path_idx,path",
        list(enumerate(suite.unique_paths)),
        ids=[
            f"path {i + 1}: {p.description}"
            for i, p in enumerate(suite.unique_paths)
        ],
    )
    def test_path(self, path_idx, path):
        results = suite.validate_path(path)
        report = ValidationReport(f"path {path_idx + 1}", results)
        assert report.all_passed, f"\n{report.render()}"
