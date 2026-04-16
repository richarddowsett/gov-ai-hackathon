#!/usr/bin/env bash
# =============================================================================
# Journey Contract Validator — Test Runner
#
# Usage:
#   ./scripts/validate-journey.sh                      # run all tests (defaults)
#   ./scripts/validate-journey.sh contract              # run contract spec only
#   ./scripts/validate-journey.sh drift                 # run drift detection demo
#   ./scripts/validate-journey.sh all                   # run everything
#
# Customise the journey, prototype, and service under test:
#   JOURNEY_JSON=path/to/journey.json \
#   PROTOTYPE_DIR=path/to/prototype \
#   SERVICE_JSON=path/to/routes.json \
#   ./scripts/validate-journey.sh
#
# Or inline:
#   ./scripts/validate-journey.sh --journey my/journey.json \
#                                 --prototype my/proto \
#                                 --service my/routes.json
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

JOURNEY="${JOURNEY_JSON:-}"
PROTO="${PROTOTYPE_DIR:-}"
SERVICE="${SERVICE_JSON:-}"
TARGET="all"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --journey)   JOURNEY="$2"; shift 2 ;;
    --prototype) PROTO="$2";   shift 2 ;;
    --service)   SERVICE="$2"; shift 2 ;;
    contract|drift|all) TARGET="$1"; shift ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

SBT_OPTS=""
[[ -n "$JOURNEY" ]] && SBT_OPTS="$SBT_OPTS -Djourney.json=$JOURNEY"
[[ -n "$PROTO"   ]] && SBT_OPTS="$SBT_OPTS -Dprototype.dir=$PROTO"
[[ -n "$SERVICE" ]] && SBT_OPTS="$SBT_OPTS -Dservice.json=$SERVICE"

echo "╔══════════════════════════════════════════════════════╗"
echo "║  Journey Contract Validator                         ║"
echo "║  journey   : ${JOURNEY:-example/journey.json (default)}"
echo "║  prototype : ${PROTO:-prototype (default)}"
echo "║  service   : ${SERVICE:-service/routes.json (default)}"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

case "$TARGET" in
  contract)
    echo "=== Running Contract Validation ==="
    sbt $SBT_OPTS "testOnly contract.JourneyContractSpec"
    ;;
  drift)
    echo "=== Running Drift Detection Demo ==="
    sbt $SBT_OPTS "testOnly contract.DriftDetectionSpec"
    ;;
  all)
    echo "=== Running All Tests ==="
    sbt $SBT_OPTS test
    ;;
esac

echo ""
echo "=== Done ==="
