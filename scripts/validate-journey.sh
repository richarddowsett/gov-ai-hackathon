#!/usr/bin/env bash
# =============================================================================
# Journey Contract Validator — Test Runner
#
# Usage:
#   ./scripts/validate-journey.sh           # run all tests
#   ./scripts/validate-journey.sh prototype  # validate prototype only
#   ./scripts/validate-journey.sh service    # validate service only
#   ./scripts/validate-journey.sh drift      # run drift detection demo
#   ./scripts/validate-journey.sh parser     # run parser tests only
#   ./scripts/validate-journey.sh graph      # run graph traversal tests only
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

TARGET="${1:-all}"

case "$TARGET" in
  prototype)
    echo "=== Validating Prototype against Journey Contract ==="
    sbt "testOnly contract.PrototypeContractSpec"
    ;;
  service)
    echo "=== Validating Service against Journey Contract ==="
    sbt "testOnly contract.ServiceContractSpec"
    ;;
  drift)
    echo "=== Running Drift Detection Demo ==="
    sbt "testOnly contract.DriftDetectionSpec"
    ;;
  parser)
    echo "=== Running Parser Tests ==="
    sbt "testOnly contract.JourneyParserSpec"
    ;;
  graph)
    echo "=== Running Graph Traversal Tests ==="
    sbt "testOnly contract.JourneyGraphSpec"
    ;;
  all)
    echo "=== Running All Journey Contract Validation Tests ==="
    sbt test
    ;;
  *)
    echo "Usage: $0 {all|prototype|service|drift|parser|graph}"
    exit 1
    ;;
esac

echo ""
echo "=== Done ==="
