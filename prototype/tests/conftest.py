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
    storage_url = os.environ.get("JOURNEY_STORAGE_URL", "http://localhost:9000")
    service_name = os.environ.get("JOURNEY_SERVICE_NAME", "example-survey")
    prototype_dir = os.path.join(PROJECT_ROOT, "prototype")
    fallback_path = os.path.join(PROJECT_ROOT, "example", "journey.json")

    return PrototypeTestSuite.from_storage(
        storage_url=storage_url,
        service_name=service_name,
        prototype_dir=prototype_dir,
        fallback_path=fallback_path,
    )
