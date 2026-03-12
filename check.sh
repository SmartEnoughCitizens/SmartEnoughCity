#!/usr/bin/env bash
# =============================================================================
# check.sh — SmartEnoughCity local CI checks
#
# Usage:
#   ./check.sh                  # run everything
#   ./check.sh --python         # Python linter + tests only
#   ./check.sh --java           # Hermes Gradle check only
#   ./check.sh --frontend       # Frontend typecheck / lint / format only
#   ./check.sh --python --java  # combine any subset
#
# Exit code: 0 only if every selected step passes.
# =============================================================================

set -euo pipefail

# ── Resolve project root (directory containing this script) ──────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
RESET='\033[0m'

# ── Argument parsing ──────────────────────────────────────────────────────────
RUN_PYTHON=false
RUN_JAVA=false
RUN_FRONTEND=false

if [[ $# -eq 0 ]]; then
  RUN_PYTHON=true
  RUN_JAVA=true
  RUN_FRONTEND=true
else
  for arg in "$@"; do
    case "$arg" in
      --python)   RUN_PYTHON=true ;;
      --java)     RUN_JAVA=true ;;
      --frontend) RUN_FRONTEND=true ;;
      --help|-h)
        sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
        exit 0
        ;;
      *)
        echo -e "${RED}Unknown flag: $arg${RESET}"
        echo "Usage: $0 [--python] [--java] [--frontend]"
        exit 1
        ;;
    esac
  done
fi

# ── Helpers ───────────────────────────────────────────────────────────────────
declare -a RESULTS=()   # "label|PASS|time" or "label|FAIL|time"
OVERALL_STATUS=0

print_header() {
  echo ""
  echo -e "${BOLD}${BLUE}══════════════════════════════════════════════════${RESET}"
  echo -e "${BOLD}${BLUE}  $1${RESET}"
  echo -e "${BOLD}${BLUE}══════════════════════════════════════════════════${RESET}"
}

print_step() {
  echo -e "\n${CYAN}▶ $1${RESET}"
}

# run_step <label> <command...>
# Runs a command, records pass/fail + wall-clock time.
run_step() {
  local label="$1"
  shift
  print_step "$label"
  echo -e "${DIM}\$ $*${RESET}"

  local start
  start=$(date +%s)
  local status=0

  "$@" || status=$?

  local elapsed=$(( $(date +%s) - start ))

  if [[ $status -eq 0 ]]; then
    echo -e "${GREEN}✔ PASSED${RESET}  ${DIM}(${elapsed}s)${RESET}"
    RESULTS+=("${label}|PASS|${elapsed}s")
  else
    echo -e "${RED}✘ FAILED${RESET}  ${DIM}(${elapsed}s)${RESET}"
    RESULTS+=("${label}|FAIL|${elapsed}s")
    OVERALL_STATUS=1
  fi
}

# ── Python checks ─────────────────────────────────────────────────────────────
if [[ "$RUN_PYTHON" == "true" ]]; then
  PYTHON_PACKAGES=(
    "backend/data_handler"
    "backend/inference_engine"
  )

  for pkg in "${PYTHON_PACKAGES[@]}"; do
    PKG_DIR="$SCRIPT_DIR/$pkg"
    PKG_NAME="$(basename "$pkg")"

    print_header "Python · $PKG_NAME"

    if [[ ! -d "$PKG_DIR" ]]; then
      echo -e "${YELLOW}⚠ Directory not found: $PKG_DIR — skipping${RESET}"
      RESULTS+=("$PKG_NAME · (missing)|SKIP|0s")
      continue
    fi

    if [[ ! -f "$PKG_DIR/pyproject.toml" ]]; then
      echo -e "${YELLOW}⚠ No pyproject.toml in $PKG_DIR — skipping${RESET}"
      RESULTS+=("$PKG_NAME · (no pyproject.toml)|SKIP|0s")
      continue
    fi

    pushd "$PKG_DIR" > /dev/null

    run_step "$PKG_NAME · ruff lint"   uv run ruff check .
    run_step "$PKG_NAME · ruff format" uv run ruff format .
    run_step "$PKG_NAME · pytest"      uv run pytest

    popd > /dev/null
  done
fi

# ── Java / Hermes checks ──────────────────────────────────────────────────────
if [[ "$RUN_JAVA" == "true" ]]; then
  HERMES_DIR="$SCRIPT_DIR/backend/hermes"

  print_header "Java · Hermes"

  if [[ ! -d "$HERMES_DIR" ]]; then
    echo -e "${YELLOW}⚠ Hermes directory not found: $HERMES_DIR — skipping${RESET}"
    RESULTS+=("hermes · (missing)|SKIP|0s")
  else
    pushd "$HERMES_DIR" > /dev/null

    # Make sure gradlew is executable (can be lost on Windows checkouts)
    [[ -f "./gradlew" ]] && chmod +x ./gradlew

    run_step "hermes · gradle check"         ./gradlew check -x spotbugsMain
    run_step "hermes · gradle spotlessApply" ./gradlew spotlessApply

    popd > /dev/null
  fi
fi

# ── Frontend checks ───────────────────────────────────────────────────────────
if [[ "$RUN_FRONTEND" == "true" ]]; then
  FRONTEND_DIR="$SCRIPT_DIR/frontend"

  print_header "Frontend"

  if [[ ! -d "$FRONTEND_DIR" ]]; then
    echo -e "${YELLOW}⚠ Frontend directory not found: $FRONTEND_DIR — skipping${RESET}"
    RESULTS+=("frontend · (missing)|SKIP|0s")
  else
    pushd "$FRONTEND_DIR" > /dev/null

    # Install deps if node_modules is absent (first-time setup)
    if [[ ! -d "node_modules" ]]; then
      echo -e "${YELLOW}⚠ node_modules not found — running pnpm install first${RESET}"
      pnpm install --frozen-lockfile
    fi

    run_step "frontend · typecheck" pnpm run typecheck
    run_step "frontend · lint"      pnpm run lint
    run_step "frontend · format"    pnpm run format

    popd > /dev/null
  fi
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${BLUE}══════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}${BLUE}  Summary${RESET}"
echo -e "${BOLD}${BLUE}══════════════════════════════════════════════════${RESET}"

PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0

for entry in "${RESULTS[@]}"; do
  IFS='|' read -r label status timing <<< "$entry"
  if [[ "$status" == "PASS" ]]; then
    echo -e "  ${GREEN}✔ PASS${RESET}  ${label}  ${DIM}${timing}${RESET}"
    (( ++PASS_COUNT ))
  elif [[ "$status" == "FAIL" ]]; then
    echo -e "  ${RED}✘ FAIL${RESET}  ${label}  ${DIM}${timing}${RESET}"
    (( ++FAIL_COUNT ))
  else
    echo -e "  ${YELLOW}– SKIP${RESET}  ${label}"
    (( ++SKIP_COUNT ))
  fi
done

echo ""
echo -e "${DIM}Passed: ${PASS_COUNT}  Failed: ${FAIL_COUNT}  Skipped: ${SKIP_COUNT}${RESET}"
echo ""

if [[ $OVERALL_STATUS -eq 0 ]]; then
  echo -e "${BOLD}${GREEN}All checks passed — safe to open a PR 🚀${RESET}"
else
  echo -e "${BOLD}${RED}One or more checks failed — fix the issues above before opening a PR.${RESET}"
fi

echo ""
exit $OVERALL_STATUS
