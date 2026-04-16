#!/usr/bin/env bash
# =============================================================================
# Journey Contract Validator — Test Runner
#
# Usage:
#   ./scripts/validate-journey.sh                # run all tests
#   ./scripts/validate-journey.sh journey        # run journey validation only
#   ./scripts/validate-journey.sh all            # run everything
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

TARGET="all"

while [[ $# -gt 0 ]]; do
  case "$1" in
    journey|all) TARGET="$1"; shift ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

echo "╔══════════════════════════════════════════════════════╗"
echo "║  Journey Contract Validator                         ║"
echo "║  journey  : example-service/conf/journey.json       ║"
echo "║  service  : example-service (Play Framework)        ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

case "$TARGET" in
  journey)
    echo "=== Running Journey Validation ==="
    sbt "exampleService/testOnly JourneyValidationSpec"
    ;;
  all)
    echo "=== Running All Tests ==="
    sbt test
    ;;
esac

echo ""
echo "=== Done ==="
