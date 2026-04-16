#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FILE="service/routes.json"

restore() {
  perl -i -pe 's/"false": "page-6"/"false": "page-7"/g' "$FILE"
}
trap restore EXIT

echo "== Baseline (service should pass) =="
./scripts/validate-journey service

echo "== Injecting service drift =="
perl -i -pe 's/"false": "page-7"/"false": "page-6"/g' "$FILE"

set +e
./scripts/validate-journey service
EXIT_CODE=$?
set -e

if [ "$EXIT_CODE" -eq 0 ]; then
  echo "Expected service validation failure, but it passed"
  exit 1
fi

echo "== Service drift detected as expected =="
restore
./scripts/validate-journey service

echo "Service drift demo complete"
