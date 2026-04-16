"""Client for fetching journey JSON from the journey-storage API."""

import json
import urllib.request
import urllib.error


def fetch_journey(storage_url: str, service_name: str) -> str:
    """Fetch journey JSON string from the storage API.

    Args:
        storage_url: Base URL of the storage API, e.g. "http://localhost:9000"
        service_name: The service name key stored in the database

    Returns:
        The raw journey JSON string (contents of the ``json`` field)

    Raises:
        ConnectionError: If the storage API cannot be reached
        ValueError: If the response is missing the ``json`` field
        RuntimeError: If the API returns a non-200 status
    """
    url = f"{storage_url.rstrip('/')}/journeys/{service_name}"
    try:
        with urllib.request.urlopen(url) as response:
            data = json.loads(response.read().decode())
            if "json" not in data:
                raise ValueError(f"Response missing 'json' field: {str(data)[:200]}")
            return data["json"]
    except urllib.error.URLError as e:
        raise ConnectionError(f"Cannot connect to storage API at {storage_url}: {e}") from e
    except urllib.error.HTTPError as e:
        raise RuntimeError(
            f"Storage API returned {e.code}: {e.read().decode()[:200]}"
        ) from e
