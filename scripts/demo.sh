#!/usr/bin/env bash
# =============================================================================
# Journey Contract Validator — Demo Script
#
# Walks through the full story:
#   1. Shows the journey JSON (the single source of truth)
#   2. Validates the prototype + service against it (should pass)
#   3. Introduces drift (changes a prototype page title)
#   4. Re-validates (should fail with clear diff)
#   5. Reverts the change (restores the original)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

TMPOUT=$(mktemp)
trap "rm -f '$TMPOUT'" EXIT

# Extracts and colorises the interesting lines from sbt test output.
show_results() {
  local file="$1"
  local mode="${2:-all}"

  # Strip ANSI and sbt prefixes into a clean temp file
  local cleaned
  cleaned=$(mktemp)
  sed 's/\x1b\[[0-9;]*m//g' "$file" \
    | sed 's/^\[info\]  *+ *//' \
    | sed 's/^\[info\]  *//' \
    | sed 's/^\[error\]  *//' > "$cleaned"

  if [[ "$mode" == "summary" ]]; then
    grep -E "(passed,.*failed|All tests passed|CHECKS PASSED)" "$cleaned" | while IFS= read -r line; do
      case "$line" in
        *"All tests passed"*|*"CHECKS PASSED"*)
          echo -e "  ${GREEN}${BOLD}${line}${NC}" ;;
        *)
          echo -e "  ${line}" ;;
      esac
    done
    rm -f "$cleaned"
    return
  fi

  # For failure mode: show one clean failure block, then the test summary
  local shown_failure=false
  while IFS= read -r line; do
    case "$line" in
      *"*** FAILED ***"*)
        if [[ "$shown_failure" == false ]]; then
          echo -e "  ${RED}${BOLD}${line}${NC}"
        fi
        ;;
      *"❌"*"— title"*|*"❌"*"— text input"*|*"❌"*"— radio"*|*"❌"*"— option"*|*"❌"*"— checkbox"*|*"❌"*"— route"*|*"❌"*"— type"*|*"❌"*"— question"*|*"❌"*"— branching"*)
        if [[ "$shown_failure" == false ]]; then
          echo -e "  ${RED}${BOLD}${line}${NC}"
        fi
        ;;
      *"expected :"*)
        if [[ "$shown_failure" == false ]]; then
          echo -e "    ${DIM}${line}${NC}"
        fi
        ;;
      *"actual   :"*)
        if [[ "$shown_failure" == false ]]; then
          echo -e "    ${RED}${line}${NC}"
        fi
        ;;
      *"contract :"*)
        if [[ "$shown_failure" == false ]]; then
          echo -e "    ${CYAN}${line}${NC}"
        fi
        ;;
      *"fix at   :"*)
        if [[ "$shown_failure" == false ]]; then
          echo -e "    ${YELLOW}${BOLD}${line}${NC}"
          shown_failure=true
        fi
        ;;
      *"TESTS FAILED"*)
        echo ""
        echo -e "  ${RED}${BOLD}${line}${NC}"
        ;;
    esac
  done < "$cleaned"

  rm -f "$cleaned"
}

echo ""
echo -e "${BOLD}=============================================${NC}"
echo -e "${BOLD}   AI Journey Contract Validator — Demo${NC}"
echo -e "${BOLD}=============================================${NC}"
echo ""

# --- Step 1: Show the journey ---
echo -e "${YELLOW}${BOLD}Step 1: The Journey JSON (single source of truth)${NC}"
echo "---------------------------------------------"
echo -e "File: ${CYAN}example/journey.json${NC}"
echo ""
echo "Pages in the journey:"
python3 -c "
import json
with open('example/journey.json') as f:
    j = json.load(f)
for i, p in enumerate(j['pages']):
    idx = p['index']
    if isinstance(idx, dict):
        branches = ', '.join(f'{k}->{v}' for k,v in idx.items())
        print(f'  [{i}] {p[\"type\"]:25s} \"{p[\"title\"]}\"  -> branches: {branches}')
    else:
        print(f'  [{i}] {p[\"type\"]:25s} \"{p[\"title\"]}\"  -> next: {idx}')
" 2>/dev/null || echo "  (install python3 to see journey summary, or view example/journey.json)"
echo ""

# --- Step 2: Validate everything (should pass) ---
echo -e "${YELLOW}${BOLD}Step 2: Validating prototype + service — should PASS${NC}"
echo "---------------------------------------------"
if sbt "testOnly contract.JourneyContractSpec" > "$TMPOUT" 2>&1; then
  show_results "$TMPOUT" "summary"
  echo -e "  ${GREEN}${BOLD}✅ All contract validation passed!${NC}"
else
  show_results "$TMPOUT" "all"
  echo -e "  ${RED}Unexpected failure — see output above.${NC}"
fi
echo ""

# --- Step 3: Introduce drift ---
echo -e "${YELLOW}${BOLD}Step 3: Introducing drift — changing a page title in the prototype${NC}"
echo "---------------------------------------------"
ORIGINAL_FILE="prototype/page-1-name.html"
BACKUP_FILE="prototype/page-1-name.html.bak"

cp "$ORIGINAL_FILE" "$BACKUP_FILE"
sed -i.tmp 's/What is your name?/What'\''s your name?/g' "$ORIGINAL_FILE"
rm -f "${ORIGINAL_FILE}.tmp"

echo -e "  Changed in ${CYAN}${ORIGINAL_FILE}${NC}:"
echo -e "    ${DIM}before:${NC} What is your name?"
echo -e "    ${RED}after:${NC}  What's your name?"
echo ""

# --- Step 4: Re-validate (should fail) ---
echo -e "${YELLOW}${BOLD}Step 4: Re-validating — should FAIL with drift detected${NC}"
echo "---------------------------------------------"
# 'clean' forces sbt to re-run tests (otherwise it caches results when source is unchanged)
if sbt clean "testOnly contract.JourneyContractSpec" > "$TMPOUT" 2>&1; then
  show_results "$TMPOUT" "all"
  echo -e "  ${RED}Expected failure but tests passed — something is wrong.${NC}"
else
  echo ""
  show_results "$TMPOUT" "failures"
  echo ""
  echo -e "  ${GREEN}${BOLD}Drift detected! The validator caught the mismatch.${NC}"
fi
echo ""

# --- Step 5: Revert the change ---
echo -e "${YELLOW}${BOLD}Step 5: Reverting the change${NC}"
echo "---------------------------------------------"
mv "$BACKUP_FILE" "$ORIGINAL_FILE"
echo -e "  Reverted ${CYAN}${ORIGINAL_FILE}${NC} to original."
echo ""

echo -e "${BOLD}=============================================${NC}"
echo -e "${BOLD}   Demo complete!${NC}"
echo ""
echo "   Key takeaway: The journey JSON is the"
echo "   single source of truth. Any drift between"
echo "   the contract and the implementation is"
echo "   caught automatically."
echo -e "${BOLD}=============================================${NC}"
