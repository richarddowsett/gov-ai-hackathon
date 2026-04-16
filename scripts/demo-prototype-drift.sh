#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FILE="prototype/page-2.html"

restore() {
  perl -i -pe "s/What\\x27s your name\\?/What is your name\\?/g" "$FILE"
}
trap restore EXIT

echo "== Baseline (prototype should pass) =="
./scripts/validate-journey prototype

echo "== Injecting prototype drift =="
perl -i -pe "s/What is your name\\?/What\\x27s your name\\?/g" "$FILE"

set +e
./scripts/validate-journey prototype
EXIT_CODE=$?
set -e

if [ "$EXIT_CODE" -eq 0 ]; then
  echo "Expected prototype validation failure, but it passed"
  exit 1
fi

echo "== Prototype drift detected as expected =="
restore
./scripts/validate-journey prototype

echo "Prototype drift demo complete"
