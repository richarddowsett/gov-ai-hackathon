#!/usr/bin/env bash
set -euo pipefail

echo "== Running baseline validation with Scala =="
./scripts/validate-journey all

echo "== Introducing drift in prototype title =="
perl -i -pe 's/Enter your name/What\x27s your name\?/g' prototype/start.html

set +e
./scripts/validate-journey prototype
EXIT_CODE=$?
set -e

if [ "$EXIT_CODE" -eq 0 ]; then
  echo "Expected failure did not happen"
  exit 1
fi

echo "== Restoring prototype title =="
perl -i -pe 's/What\x27s your name\?/Enter your name/g' prototype/start.html

./scripts/validate-journey prototype

echo "Demo complete"
