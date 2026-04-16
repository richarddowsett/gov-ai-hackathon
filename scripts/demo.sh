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
NC='\033[0m'

echo ""
echo "============================================="
echo "   AI Journey Contract Validator — Demo"
echo "============================================="
echo ""

# --- Step 1: Show the journey ---
echo -e "${YELLOW}Step 1: The Journey JSON (single source of truth)${NC}"
echo "---------------------------------------------"
echo "File: example/journey.json"
echo ""
echo "Pages in the journey:"
python3 -c "
import json, sys
with open('example/journey.json') as f:
    j = json.load(f)
for i, p in enumerate(j['pages']):
    idx = p['index']
    if isinstance(idx, dict):
        branches = ', '.join(f'{k}->{v}' for k,v in idx.items())
        print(f'  [{i}] {p[\"type\"]:25s} \"{p[\"title\"]}\"  -> branches: {branches}')
    else:
        print(f'  [{i}] {p[\"type\"]:25s} \"{p[\"title\"]}\"  -> next: {idx}')
" 2>/dev/null || echo "  (install python3 to see the journey summary, or view example/journey.json directly)"
echo ""

# --- Step 2: Validate everything (should pass) ---
echo -e "${YELLOW}Step 2: Validating prototype + service — should PASS${NC}"
echo "---------------------------------------------"
if sbt "testOnly contract.JourneyContractSpec" 2>&1 | tail -10; then
  echo -e "${GREEN}All contract validation passed!${NC}"
else
  echo -e "${RED}Unexpected failure — check the test output above.${NC}"
fi
echo ""

# --- Step 3: Introduce drift ---
echo -e "${YELLOW}Step 3: Introducing drift — changing a page title in the prototype${NC}"
echo "---------------------------------------------"
ORIGINAL_FILE="prototype/page-1-name.html"
BACKUP_FILE="prototype/page-1-name.html.bak"

cp "$ORIGINAL_FILE" "$BACKUP_FILE"
sed -i.tmp 's/What is your name?/What'\''s your name?/g' "$ORIGINAL_FILE"
rm -f "${ORIGINAL_FILE}.tmp"

echo "Changed 'What is your name?' → 'What's your name?' in $ORIGINAL_FILE"
echo ""

# --- Step 4: Re-validate (should fail) ---
echo -e "${YELLOW}Step 4: Re-validating — should FAIL with drift detected${NC}"
echo "---------------------------------------------"
# 'clean' forces sbt to re-run tests (otherwise it caches results when source is unchanged)
if sbt clean "testOnly contract.JourneyContractSpec" 2>&1 | tail -20; then
  echo -e "${RED}Expected failure but tests passed — something is wrong.${NC}"
else
  echo -e "${GREEN}Drift detected! The validator caught the mismatch.${NC}"
fi
echo ""

# --- Step 5: Revert the change ---
echo -e "${YELLOW}Step 5: Reverting the change${NC}"
echo "---------------------------------------------"
mv "$BACKUP_FILE" "$ORIGINAL_FILE"
echo "Reverted $ORIGINAL_FILE to original."
echo ""

echo "============================================="
echo "   Demo complete!"
echo ""
echo "   Key takeaway: The journey JSON is the"
echo "   single source of truth. Any drift between"
echo "   the contract and the implementation is"
echo "   caught automatically."
echo "============================================="
