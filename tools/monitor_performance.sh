#!/bin/bash
# Performance monitoring script for Windscribe VPN Android app

PACKAGE="com.windscribe.vpn"
DURATION=60  # seconds
OUTPUT_DIR="performance_logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "================================================"
echo "  Windscribe VPN Performance Monitor"
echo "================================================"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}Error: No Android device connected${NC}"
    echo "Please connect a device or start an emulator"
    exit 1
fi

# Check if app is installed
if ! adb shell pm list packages | grep -q "$PACKAGE"; then
    echo -e "${RED}Error: $PACKAGE is not installed${NC}"
    exit 1
fi

# Check if app is running
if ! adb shell pidof "$PACKAGE" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: App is not currently running${NC}"
    echo "Starting app..."
    adb shell am start -n "$PACKAGE/.mobile.ui.AppStartActivity" > /dev/null 2>&1
    sleep 3
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo -e "${GREEN}✓${NC} Device connected"
echo -e "${GREEN}✓${NC} App running (PID: $(adb shell pidof $PACKAGE))"
echo ""
echo "Starting performance monitoring for $DURATION seconds..."
echo "Please interact with the app normally (scroll lists, switch tabs, connect VPN, etc.)"
echo ""

# Clear previous frame stats
adb shell dumpsys gfxinfo "$PACKAGE" reset > /dev/null 2>&1

# Show countdown
for ((i=$DURATION; i>0; i-=10)); do
    echo "  Time remaining: ${i}s..."
    sleep 10
done

echo ""
echo "Collecting performance data..."

# Capture frame stats
adb shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUTPUT_DIR/framestats_$TIMESTAMP.txt" 2>&1

# Capture general gfx info
adb shell dumpsys gfxinfo "$PACKAGE" > "$OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt" 2>&1

# Capture system memory
adb shell dumpsys meminfo "$PACKAGE" > "$OUTPUT_DIR/meminfo_$TIMESTAMP.txt" 2>&1

# Capture CPU usage
adb shell top -n 1 | grep "$PACKAGE" > "$OUTPUT_DIR/cpu_$TIMESTAMP.txt" 2>&1

# Capture logcat for frame metrics and recomposition logs
adb logcat -d -s "frame_metrics:*" "recomposition:*" "Choreographer:*" > "$OUTPUT_DIR/logcat_$TIMESTAMP.txt" 2>&1

echo ""
echo "================================================"
echo "  Performance Analysis Results"
echo "================================================"
echo ""

# Parse jank statistics
echo "📊 Frame Rendering Statistics:"
echo "-----------------------------------"

TOTAL_FRAMES=$(grep "Total frames rendered:" "$OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt" | awk '{print $4}')
JANKY_FRAMES=$(grep "Janky frames:" "$OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt" | awk '{print $3}')
PERCENTILE_50=$(grep "50th percentile:" "$OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt" | awk '{print $3}' | sed 's/ms//')
PERCENTILE_90=$(grep "90th percentile:" "$OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt" | awk '{print $3}' | sed 's/ms//')
PERCENTILE_95=$(grep "95th percentile:" "$OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt" | awk '{print $3}' | sed 's/ms//')
PERCENTILE_99=$(grep "99th percentile:" "$OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt" | awk '{print $3}' | sed 's/ms//')

if [ -n "$TOTAL_FRAMES" ] && [ "$TOTAL_FRAMES" -gt 0 ]; then
    if [ -n "$JANKY_FRAMES" ]; then
        JANK_PERCENT=$(echo "scale=2; $JANKY_FRAMES * 100 / $TOTAL_FRAMES" | bc)
        AVG_FPS=$(echo "scale=1; $TOTAL_FRAMES / $DURATION" | bc)

        echo "  Total Frames: $TOTAL_FRAMES"
        echo "  Average FPS: $AVG_FPS"
        echo "  Janky Frames: $JANKY_FRAMES ($JANK_PERCENT%)"

        # Color code jank percentage
        if (( $(echo "$JANK_PERCENT < 5" | bc -l) )); then
            echo -e "  Performance: ${GREEN}EXCELLENT${NC} (< 5% jank)"
        elif (( $(echo "$JANK_PERCENT < 10" | bc -l) )); then
            echo -e "  Performance: ${GREEN}GOOD${NC} (< 10% jank)"
        elif (( $(echo "$JANK_PERCENT < 20" | bc -l) )); then
            echo -e "  Performance: ${YELLOW}FAIR${NC} (10-20% jank)"
        else
            echo -e "  Performance: ${RED}NEEDS IMPROVEMENT${NC} (> 20% jank)"
        fi
    else
        echo "  Total Frames: $TOTAL_FRAMES"
        echo "  Janky Frames: Not available"
    fi
else
    echo -e "  ${YELLOW}No frame data collected. App may not have been active.${NC}"
fi

echo ""
echo "⏱️  Frame Time Percentiles:"
echo "-----------------------------------"
if [ -n "$PERCENTILE_50" ]; then
    echo "  50th percentile: ${PERCENTILE_50}ms (target: < 16ms for 60fps)"
    echo "  90th percentile: ${PERCENTILE_90}ms"
    echo "  95th percentile: ${PERCENTILE_95}ms"
    echo "  99th percentile: ${PERCENTILE_99}ms"

    # Analyze frame time quality
    if (( $(echo "$PERCENTILE_95 < 16.67" | bc -l) )); then
        echo -e "  Frame Quality: ${GREEN}60+ FPS${NC}"
    elif (( $(echo "$PERCENTILE_95 < 33.33" | bc -l) )); then
        echo -e "  Frame Quality: ${YELLOW}30-60 FPS${NC}"
    else
        echo -e "  Frame Quality: ${RED}< 30 FPS${NC}"
    fi
else
    echo "  No percentile data available"
fi

echo ""
echo "💾 Memory Usage:"
echo "-----------------------------------"

TOTAL_PSS=$(grep "TOTAL PSS:" "$OUTPUT_DIR/meminfo_$TIMESTAMP.txt" | awk '{print $3}')
JAVA_HEAP=$(grep "Java Heap:" "$OUTPUT_DIR/meminfo_$TIMESTAMP.txt" | awk '{print $3}')
NATIVE_HEAP=$(grep "Native Heap:" "$OUTPUT_DIR/meminfo_$TIMESTAMP.txt" | awk '{print $3}')
GRAPHICS=$(grep "Graphics:" "$OUTPUT_DIR/meminfo_$TIMESTAMP.txt" | awk '{print $2}')

if [ -n "$TOTAL_PSS" ]; then
    TOTAL_MB=$(echo "scale=1; $TOTAL_PSS / 1024" | bc)
    echo "  Total Memory (PSS): ${TOTAL_MB} MB"
fi

if [ -n "$JAVA_HEAP" ]; then
    JAVA_MB=$(echo "scale=1; $JAVA_HEAP / 1024" | bc)
    echo "  Java Heap: ${JAVA_MB} MB"
fi

if [ -n "$NATIVE_HEAP" ]; then
    NATIVE_MB=$(echo "scale=1; $NATIVE_HEAP / 1024" | bc)
    echo "  Native Heap: ${NATIVE_MB} MB"
fi

if [ -n "$GRAPHICS" ]; then
    GFX_MB=$(echo "scale=1; $GRAPHICS / 1024" | bc)
    echo "  Graphics: ${GFX_MB} MB"
fi

echo ""
echo "🔍 Slow Frame Analysis:"
echo "-----------------------------------"

# Count frames by duration in framestats
if [ -f "$OUTPUT_DIR/framestats_$TIMESTAMP.txt" ]; then
    SLOW_COUNT=$(grep -E "^[0-9]" "$OUTPUT_DIR/framestats_$TIMESTAMP.txt" | awk '{
        total = 0
        for(i=1; i<=NF; i++) total += $i
        if(total/1000000 > 16.67) print total/1000000
    }' | wc -l | xargs)

    VERY_SLOW_COUNT=$(grep -E "^[0-9]" "$OUTPUT_DIR/framestats_$TIMESTAMP.txt" | awk '{
        total = 0
        for(i=1; i<=NF; i++) total += $i
        if(total/1000000 > 32) print total/1000000
    }' | wc -l | xargs)

    if [ -n "$SLOW_COUNT" ] && [ "$SLOW_COUNT" -gt 0 ]; then
        echo "  Frames > 16.67ms (missed 60fps): $SLOW_COUNT"
        echo "  Frames > 32ms (< 30fps): $VERY_SLOW_COUNT"

        # Find slowest frame
        SLOWEST=$(grep -E "^[0-9]" "$OUTPUT_DIR/framestats_$TIMESTAMP.txt" | awk '{
            total = 0
            for(i=1; i<=NF; i++) total += $i
            print total/1000000
        }' | sort -n | tail -1)

        if [ -n "$SLOWEST" ]; then
            echo "  Slowest frame: ${SLOWEST}ms"
        fi
    else
        echo -e "  ${GREEN}No slow frames detected!${NC}"
    fi
fi

echo ""
echo "📁 Detailed logs saved to:"
echo "-----------------------------------"
echo "  $OUTPUT_DIR/gfxinfo_$TIMESTAMP.txt     (Frame rendering stats)"
echo "  $OUTPUT_DIR/framestats_$TIMESTAMP.txt  (Per-frame breakdown)"
echo "  $OUTPUT_DIR/meminfo_$TIMESTAMP.txt     (Memory allocation)"
echo "  $OUTPUT_DIR/logcat_$TIMESTAMP.txt      (Performance logs)"
echo "  $OUTPUT_DIR/cpu_$TIMESTAMP.txt         (CPU usage)"
echo ""
echo "================================================"
echo ""

# Generate recommendations
echo "💡 Recommendations:"
echo "-----------------------------------"

if [ -n "$JANK_PERCENT" ]; then
    if (( $(echo "$JANK_PERCENT > 15" | bc -l) )); then
        echo "  ⚠️  High jank detected (${JANK_PERCENT}%)"
        echo "     - Check logcat for excessive recompositions"
        echo "     - Profile with Android Studio to find bottlenecks"
        echo "     - Review recent changes to UI components"
    fi
fi

if [ -n "$PERCENTILE_95" ]; then
    if (( $(echo "$PERCENTILE_95 > 20" | bc -l) )); then
        echo "  ⚠️  Slow frame rendering (95th percentile: ${PERCENTILE_95}ms)"
        echo "     - Check for heavy operations on main thread"
        echo "     - Review LazyColumn/Pager configurations"
        echo "     - Consider reducing animation complexity"
    fi
fi

if [ -n "$TOTAL_MB" ]; then
    if (( $(echo "$TOTAL_MB > 300" | bc -l) )); then
        echo "  ⚠️  High memory usage (${TOTAL_MB}MB)"
        echo "     - Check for memory leaks with LeakCanary"
        echo "     - Review image caching strategies"
        echo "     - Consider lazy loading for large lists"
    fi
fi

if [ -n "$JANK_PERCENT" ] && (( $(echo "$JANK_PERCENT < 5" | bc -l) )); then
    if [ -n "$PERCENTILE_95" ] && (( $(echo "$PERCENTILE_95 < 16.67" | bc -l) )); then
        echo -e "  ${GREEN}✓ Performance is excellent! No issues detected.${NC}"
    fi
fi

echo ""
echo "To analyze frame details, run:"
echo "  cat $OUTPUT_DIR/framestats_$TIMESTAMP.txt"
echo ""
echo "To see recomposition logs, run:"
echo "  grep -E 'recomposed|JANKY' $OUTPUT_DIR/logcat_$TIMESTAMP.txt"
echo ""