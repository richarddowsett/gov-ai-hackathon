import glob
import os
from typing import List, Optional

from .graph import unique_paths as _unique_paths, JourneyPath
from .model import ValidationReport
from .page_validator import validate_html
from .parser import parse, parse_file
from .storage_client import fetch_journey


class PrototypeTestSuite:
    """High-level test helper that validates a prototype directory against a
    journey JSON contract.

    The journey can be loaded from a local file, a raw JSON string, or the
    journey-storage API.

    Usage::

        # From a local file
        suite = PrototypeTestSuite("example/journey.json", "prototype")

        # From the storage API
        suite = PrototypeTestSuite.from_storage(
            storage_url="http://localhost:9000",
            service_name="example-survey",
            prototype_dir="prototype",
        )

        # Validate a single page
        results = suite.validate_page(0)

        # Validate all pages at once
        report = suite.validate_all_pages()
        assert report.all_passed
    """

    def __init__(self, journey_path: str, prototype_dir: str):
        self.journey = parse_file(journey_path)
        self.prototype_dir = prototype_dir
        self._unique_paths = _unique_paths(self.journey)

    @classmethod
    def from_json(cls, journey_json: str, prototype_dir: str) -> "PrototypeTestSuite":
        """Create a suite from a raw journey JSON string."""
        instance = object.__new__(cls)
        instance.journey = parse(journey_json)
        instance.prototype_dir = prototype_dir
        instance._unique_paths = _unique_paths(instance.journey)
        return instance

    @classmethod
    def from_storage(
        cls,
        storage_url: str,
        service_name: str,
        prototype_dir: str,
        fallback_path: Optional[str] = None,
    ) -> "PrototypeTestSuite":
        """Create a suite by fetching journey JSON from the storage API.

        If ``fallback_path`` is provided and the storage API is unreachable,
        the journey is loaded from that local file instead.
        """
        try:
            json_str = fetch_journey(storage_url, service_name)
            return cls.from_json(json_str, prototype_dir)
        except (ConnectionError, RuntimeError, ValueError) as exc:
            if fallback_path:
                return cls(fallback_path, prototype_dir)
            raise RuntimeError(
                f"Could not fetch journey from storage API: {exc}"
            ) from exc

    @property
    def unique_paths(self) -> List[JourneyPath]:
        return self._unique_paths

    def read_page_html(self, index: int) -> str:
        pattern = os.path.join(self.prototype_dir, f"page-{index}-*.html")
        files = sorted(glob.glob(pattern))
        if not files:
            raise FileNotFoundError(
                f"No prototype page found for index {index} "
                f"(looked for {pattern})"
            )
        with open(files[0], encoding="utf-8") as f:
            return f.read()

    def validate_page(self, index: int) -> list:
        page = self.journey.pages[index]
        html = self.read_page_html(index)
        return validate_html(html, page, f"page[{index}]")

    def validate_all_pages(self) -> ValidationReport:
        results = []
        for idx in range(self.journey.page_count):
            results.extend(self.validate_page(idx))
        return ValidationReport("Prototype page validation", results)

    def validate_path(self, path: JourneyPath) -> list:
        results = []
        for step in path.steps:
            html = self.read_page_html(step.array_index)
            step_results = validate_html(html, step.page, f"page[{step.array_index}]")
            results.extend(step_results)
        return results

    def validate_all_paths(self) -> ValidationReport:
        results = []
        for path in self._unique_paths:
            results.extend(self.validate_path(path))
        return ValidationReport("Prototype path validation", results)
