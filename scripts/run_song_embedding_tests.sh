#!/usr/bin/env sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

GRADLE_USER_HOME="$REPO_ROOT/.gradle-local"
export GRADLE_USER_HOME

GRADLEW="$REPO_ROOT/gradlew"
if [ ! -x "$GRADLEW" ]; then
  GRADLEW="$REPO_ROOT/gradlew.bat"
fi

UNIT_CLASSES="
com.example.snapbadgers.EmbeddingTest
com.example.snapbadgers.songembeddings.embedding.SongEmbeddingUtilsTest
com.example.snapbadgers.songembeddings.embedding.SongEmbeddingPerformanceTest
"

UI_CLASSES="
com.example.snapbadgers.SongEmbeddingUiPerformanceTest
com.example.snapbadgers.SnapBadgersUiTest
"

build_test_args() {
  classes="$1"
  args=""
  for cls in $classes; do
    args="$args --tests $cls"
  done
  printf "%s" "$args"
}

extract_attr() {
  line="$1"
  key="$2"
  printf "%s" "$line" | sed -n "s/.*$key=\"\\([^\"]*\\)\".*/\\1/p"
}

sum_results_from_dir() {
  dir="$1"
  tests=0
  failures=0
  skipped=0
  time_sum=0

  if [ -d "$dir" ]; then
    find "$dir" -type f -name "TEST-*.xml" | while IFS= read -r f; do
      suite_line=$(grep -m1 "<testsuite " "$f" || true)
      [ -z "$suite_line" ] && continue

      t=$(extract_attr "$suite_line" "tests")
      f_count=$(extract_attr "$suite_line" "failures")
      s=$(extract_attr "$suite_line" "skipped")
      tm=$(extract_attr "$suite_line" "time")

      [ -z "$t" ] && t=0
      [ -z "$f_count" ] && f_count=0
      [ -z "$s" ] && s=0
      [ -z "$tm" ] && tm=0

      tests=$((tests + t))
      failures=$((failures + f_count))
      skipped=$((skipped + s))
      time_sum=$(awk "BEGIN{printf \"%.6f\", $time_sum + $tm}")
      echo "$tests|$failures|$skipped|$time_sum"
    done | tail -n1
  else
    echo "0|0|0|0"
  fi
}

echo "Running song embedding unit tests..."
UNIT_ARGS=$(build_test_args "$UNIT_CLASSES")
sh -c "\"$GRADLEW\" :app:testDebugUnitTest $UNIT_ARGS"
UNIT_EXIT=$?

echo "Running UI tests..."
UI_ARGS=$(build_test_args "$UI_CLASSES")
UI_LOG="$REPO_ROOT/app/build/reports/tests/ui-run.log"
mkdir -p "$(dirname "$UI_LOG")"
sh -c "\"$GRADLEW\" :app:connectedDebugAndroidTest $UI_ARGS" >"$UI_LOG" 2>&1
UI_EXIT=$?

UI_SKIPPED=false
if [ "$UI_EXIT" -ne 0 ] && grep -qi "No connected devices" "$UI_LOG"; then
  UI_SKIPPED=true
fi

UNIT_XML_DIR="$REPO_ROOT/app/build/test-results/testDebugUnitTest"
UI_XML_DIR="$REPO_ROOT/app/build/outputs/androidTest-results/connected"
REPORT_DIR="$REPO_ROOT/app/build/reports/tests"
REPORT_FILE="$REPORT_DIR/song-embedding-report.md"
mkdir -p "$REPORT_DIR"

UNIT_SUM=$(sum_results_from_dir "$UNIT_XML_DIR")
UI_SUM=$(sum_results_from_dir "$UI_XML_DIR")

UNIT_TOTAL=$(printf "%s" "$UNIT_SUM" | cut -d'|' -f1)
UNIT_FAILED=$(printf "%s" "$UNIT_SUM" | cut -d'|' -f2)
UNIT_SKIPPED=$(printf "%s" "$UNIT_SUM" | cut -d'|' -f3)
UNIT_TIME=$(printf "%s" "$UNIT_SUM" | cut -d'|' -f4)
UNIT_PASSED=$((UNIT_TOTAL - UNIT_FAILED - UNIT_SKIPPED))

UI_TOTAL=$(printf "%s" "$UI_SUM" | cut -d'|' -f1)
UI_FAILED=$(printf "%s" "$UI_SUM" | cut -d'|' -f2)
UI_SKIPPED_COUNT=$(printf "%s" "$UI_SUM" | cut -d'|' -f3)
UI_TIME=$(printf "%s" "$UI_SUM" | cut -d'|' -f4)
UI_PASSED=$((UI_TOTAL - UI_FAILED - UI_SKIPPED_COUNT))

EMBED_XML="$UNIT_XML_DIR/TEST-com.example.snapbadgers.songembeddings.embedding.SongEmbeddingPerformanceTest.xml"
EMBED_AVG_MS=""
if [ -f "$EMBED_XML" ]; then
  EMBED_CASE_TIME=$(grep -m1 "benchmarkGetEmbeddingThroughput" "$EMBED_XML" | sed -n 's/.*time="\([^"]*\)".*/\1/p')
  if [ -n "$EMBED_CASE_TIME" ]; then
    EMBED_AVG_MS=$(awk "BEGIN{printf \"%.6f\", ($EMBED_CASE_TIME * 1000.0) / 2000.0}")
  fi
fi

UI_XML=$(find "$UI_XML_DIR" -type f -name "TEST-com.example.snapbadgers.SongEmbeddingUiPerformanceTest*.xml" | head -n1)
UI_BENCH_MS=""
if [ -n "${UI_XML:-}" ] && [ -f "$UI_XML" ]; then
  UI_CASE_TIME=$(grep -m1 "benchmarkNavigationToLibrary" "$UI_XML" | sed -n 's/.*time="\([^"]*\)".*/\1/p')
  if [ -n "$UI_CASE_TIME" ]; then
    UI_BENCH_MS=$(awk "BEGIN{printf \"%.3f\", $UI_CASE_TIME * 1000.0}")
  fi
fi

STATUS="PASS"
if [ "$UNIT_EXIT" -ne 0 ] || { [ "$UI_EXIT" -ne 0 ] && [ "$UI_SKIPPED" != "true" ]; } || [ "$UNIT_FAILED" -gt 0 ] || [ "$UI_FAILED" -gt 0 ]; then
  STATUS="FAIL"
elif [ "$UI_SKIPPED" = "true" ]; then
  STATUS="PARTIAL (UI Skipped: no device)"
fi

GENERATED_AT=$(date +"%Y-%m-%d %H:%M:%S %z")

cat > "$REPORT_FILE" <<EOF
# Song Embedding + UI Test Report

- Generated at: $GENERATED_AT
- Status: **$STATUS**

## Unit Test Summary

- Classes: com.example.snapbadgers.EmbeddingTest, com.example.snapbadgers.songembeddings.embedding.SongEmbeddingUtilsTest, com.example.snapbadgers.songembeddings.embedding.SongEmbeddingPerformanceTest
- Total: $UNIT_TOTAL
- Passed: $UNIT_PASSED
- Failed: $UNIT_FAILED
- Skipped: $UNIT_SKIPPED
- Time (s): $UNIT_TIME

### Unit Test Intent and Coverage

1. **EmbeddingTest.testGetEmbedding**
   Verifies the full song-embedding pipeline for one realistic audio-feature sample.
   Checks output contract: 128 dimensions and L2-normalized vector for cosine-similarity usage.
2. **SongEmbeddingUtilsTest.buildBaseVector should scale and clamp values correctly**
   Validates base feature engineering rules.
   Confirms normalization and clamp boundaries for tempo, loudness, and duration fields.
3. **SongEmbeddingUtilsTest.addDerivedFeatures should compute interaction features**
   Validates interaction-feature math (e.g., dance*energy, valence*energy).
   Ensures derived feature values match expected hand-calculated results.
4. **SongEmbeddingUtilsTest.getEmbedding with null should return zero 128d vector**
   Validates null-safety behavior.
   Ensures missing input still returns a valid 128-d zero vector without crashes.
5. **SongEmbeddingUtilsTest.getEmbedding should be deterministic and normalized**
   Validates output consistency across repeated runs with identical input.
   Confirms stable deterministic behavior and unit-norm output.
6. **SongEmbeddingUtilsTest.normalize should return zero vector for near-zero input**
   Validates numerical stability in edge cases.
   Ensures normalization of zero input does not produce NaN/Inf values.
7. **SongEmbeddingPerformanceTest.benchmarkGetEmbeddingThroughput**
   Measures average embedding latency using warm-up and measured loops.
   Provides a performance guardrail to detect major throughput regressions.

## UI Test Summary

- Classes: com.example.snapbadgers.SongEmbeddingUiPerformanceTest, com.example.snapbadgers.SnapBadgersUiTest
- Total: $UI_TOTAL
- Passed: $UI_PASSED
- Failed: $UI_FAILED
- Skipped: $UI_SKIPPED_COUNT
- Time (s): $UI_TIME
- UI run skipped due to no connected device: $UI_SKIPPED

### UI Test Intent and Coverage

1. **SongEmbeddingUiPerformanceTest.benchmarkNavigationToLibrary**
   Measures end-to-end UI latency from tapping Library navigation to screen readiness.
   Helps track UI responsiveness over time and catch navigation slowdowns.
2. **SnapBadgersUiTest.testMainScreenInitialization**
   Validates initial render of the main screen and required primary UI elements.
   Ensures app startup surface is usable and visually complete.
3. **SnapBadgersUiTest.testNavigationToLibrary**
   Validates side-navigation behavior to the Music Library destination.
   Confirms route switch and destination content visibility.
4. **SnapBadgersUiTest.testNavigationToHistory**
   Validates side-navigation behavior to Activity History.
   Confirms expected history screen title and content surface.
5. **SnapBadgersUiTest.testNavigationToSettings**
   Validates side-navigation behavior to Settings.
   Confirms settings page and API configuration section are present.
6. **SnapBadgersUiTest.testTextInputAndAnalyzeButton**
   Validates primary text-input interaction on the main flow.
   Confirms Analyze action control remains visible/usable after text entry.

## Speed Metrics

- Song embedding average latency (estimated): ${EMBED_AVG_MS:-N/A} ms/call
- UI library navigation benchmark time: ${UI_BENCH_MS:-N/A} ms
EOF

echo "Report generated at: $REPORT_FILE"

if [ "$STATUS" = "FAIL" ]; then
  exit 1
fi
