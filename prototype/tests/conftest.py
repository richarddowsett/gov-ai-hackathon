import os
import sys

import pytest

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PROTO_VALIDATION_ROOT = os.path.join(PROJECT_ROOT, "prototype-validation")

if PROTO_VALIDATION_ROOT not in sys.path:
    sys.path.insert(0, PROTO_VALIDATION_ROOT)

from journeyvalidation import PrototypeTestSuite  # noqa: E402


@pytest.fixture(scope="session")
def suite():
    journey_path = os.path.join(PROJECT_ROOT, "example", "journey.json")
    prototype_dir = os.path.join(PROJECT_ROOT, "prototype")
    return PrototypeTestSuite(journey_path, prototype_dir)
