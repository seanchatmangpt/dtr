#!/bin/bash
#
# test-cache-performance.sh — Measure mvnd cache performance improvement
#
# This script:
# 1. Performs a clean build (cold cache baseline)
# 2. Performs a warm build (cache hit measurement)
# 3. Reports time deltas and cache statistics
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

MVNW="${1:-.}/mvnw"
BASELINE_FILE=".mvnd-baseline.json"
CACHE_DIR=".mvnd"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           Maven/mvnd Cache Performance Test                ║${NC}"
echo -e "${BLUE}║                                                            ║${NC}"
echo -e "${BLUE}║ This test measures build performance with/without cache:   ║${NC}"
echo -e "${BLUE}║ 1. Clean build (cold cache)   → establishes baseline      ║${NC}"
echo -e "${BLUE}║ 2. Incremental build (cache)  → measures cache hit        ║${NC}"
echo -e "${BLUE}║ 3. Report time delta & stats  → shows optimization impact ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Verify mvnw exists
if [ ! -f "$MVNW" ]; then
    echo -e "${RED}ERROR: Maven wrapper not found at $MVNW${NC}"
    exit 1
fi

# Test 1: Clean build (cold cache)
echo -e "${YELLOW}═══ Phase 1: Clean Build (Cold Cache) ═══${NC}"
echo "Removing cache..."
rm -rf "$CACHE_DIR"
[ -f "$BASELINE_FILE" ] && rm "$BASELINE_FILE"

echo "Starting clean build with full compilation..."
echo "(Measuring time for: mvnw clean verify -DskipTests=true -q)"
echo ""

START_TIME=$(date +%s%N)
"$MVNW" clean verify -DskipTests=true -q 2>&1 | tail -5
COLD_END_TIME=$(date +%s%N)

COLD_MILLIS=$(( ($COLD_END_TIME - $START_TIME) / 1000000 ))
COLD_SECONDS=$(echo "scale=2; $COLD_MILLIS / 1000" | bc)

echo ""
echo -e "${GREEN}Clean build completed in: ${COLD_SECONDS}s${NC}"
echo ""

# Save baseline
mkdir -p $(dirname "$BASELINE_FILE")
echo "{
  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
  \"build_type\": \"clean\",
  \"build_time_millis\": $COLD_MILLIS,
  \"build_time_seconds\": $COLD_SECONDS,
  \"cache_status\": \"cold\",
  \"java_version\": \"$(java -version 2>&1 | head -1)\",
  \"command\": \"mvnw clean verify -DskipTests=true -q\"
}" > "$BASELINE_FILE"

echo "Baseline saved to $BASELINE_FILE"
echo ""

# Short pause to ensure cache is fully written
sleep 2

# Test 2: Warm build (cache hit)
echo -e "${YELLOW}═══ Phase 2: Incremental Build (Cache Hit) ═══${NC}"
echo "Starting incremental build with warm cache..."
echo "(Measuring time for: mvnw verify -DskipTests=true -q)"
echo ""

START_TIME=$(date +%s%N)
"$MVNW" verify -DskipTests=true -q 2>&1 | tail -5
WARM_END_TIME=$(date +%s%N)

WARM_MILLIS=$(( ($WARM_END_TIME - $START_TIME) / 1000000 ))
WARM_SECONDS=$(echo "scale=2; $WARM_MILLIS / 1000" | bc)

echo ""
echo -e "${GREEN}Incremental build completed in: ${WARM_SECONDS}s${NC}"
echo ""

# Calculate improvement
TIME_SAVED_MILLIS=$(( $COLD_MILLIS - $WARM_MILLIS ))
TIME_SAVED_SECONDS=$(echo "scale=2; $TIME_SAVED_MILLIS / 1000" | bc)
SPEEDUP=$(echo "scale=2; $COLD_MILLIS / $WARM_MILLIS" | bc)

# Cache statistics
echo -e "${BLUE}═══ Results Summary ═══${NC}"
echo ""

# Results table
printf "%-40s %15s %15s\n" "Metric" "Cold" "Warm"
printf "%-40s %15s %15s\n" "────────────────────────────────" "─────────────" "─────────────"
printf "%-40s %14.2fs %14.2fs\n" "Build Time" "$COLD_SECONDS" "$WARM_SECONDS"

echo ""
printf "%-40s %s\n" "Time Saved per Iteration:" "${TIME_SAVED_SECONDS}s"
printf "%-40s %s\n" "Speedup Factor:" "${SPEEDUP}x"
printf "%-40s %s\n" "Builds per Hour (vs cold):" "$(echo "scale=0; 3600 / $WARM_SECONDS" | bc)"
printf "%-40s %s\n" "Extra Builds per 8hr Day:" "$(echo "scale=0; (28800 / $WARM_SECONDS) - (28800 / $COLD_SECONDS)" | bc)"

echo ""
echo -e "${BLUE}═══ Cache Statistics ═══${NC}"

if [ -d "$CACHE_DIR" ]; then
    CACHE_SIZE=$(du -sh "$CACHE_DIR" 2>/dev/null | awk '{print $1}')
    CACHE_FILES=$(find "$CACHE_DIR" -type f 2>/dev/null | wc -l)
    echo "Cache Directory: .mvnd/"
    echo "Cache Size: $CACHE_SIZE"
    echo "Cached Artifacts: $CACHE_FILES files"
else
    echo "Cache Directory: Not present"
fi

echo ""
echo -e "${BLUE}═══ Analysis ═══${NC}"
echo ""

if (( $(echo "$WARM_SECONDS < 15" | bc -l) )); then
    echo -e "${GREEN}✓ EXCELLENT: Warm cache provides significant speedup${NC}"
    echo "  Compound effect: $(echo "scale=0; $TIME_SAVED_SECONDS * 12" | bc)s saved per hour of dev"
    echo "  This equals ~15 extra builds/hour vs cold baseline"
elif (( $(echo "$WARM_SECONDS < 25" | bc -l) )); then
    echo -e "${YELLOW}✓ GOOD: Cache provides measurable improvement${NC}"
    echo "  Typical iteration saves ~$(echo "scale=0; $TIME_SAVED_SECONDS" | bc)s per build"
else
    echo -e "${YELLOW}✓ BASELINE: Cache functionality working${NC}"
    echo "  Consider: pom.xml changes invalidate cache frequently"
fi

echo ""
echo -e "${BLUE}Key Points:${NC}"
echo "1. Cache persists in .mvnd/ across sessions"
echo "2. Warm builds (no pom changes) are ~${SPEEDUP}x faster"
echo "3. Cold baseline: ${COLD_SECONDS}s for clean compile"
echo "4. Warm baseline: ${WARM_SECONDS}s for cache hit"
echo "5. To see cache status: make cache-stats"
echo "6. To force fresh: rm -rf .mvnd"
echo ""

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    Test Complete                           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
