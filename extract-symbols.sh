#!/bin/bash

# Extract Native Debug Symbols Script
# Run this after building release AABs to extract symbols for manual upload

set -e

echo "🔍 Extracting native debug symbols..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Output directory
OUTPUT_DIR="$HOME/Downloads"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Function to extract symbols
extract_symbols() {
    local module=$1
    local build_type="googleRelease"
    local symbol_dir="${module}/build/intermediates/native_debug_metadata/${build_type}/extract${build_type^}NativeDebugMetadata"
    local output_zip="${OUTPUT_DIR}/${module}-symbols-${TIMESTAMP}.zip"

    echo -e "\n📦 Processing ${YELLOW}${module}${NC} module..."

    # Run gradle task to extract symbols
    echo "  Running extraction task..."
    ./gradlew :${module}:extract${build_type^}NativeDebugMetadata --console=plain > /dev/null 2>&1

    # Check if symbols were extracted
    if [ ! -d "${symbol_dir}/out" ]; then
        echo -e "  ${RED}✗${NC} No symbols found in ${symbol_dir}/out"
        return 1
    fi

    # Check for critical libraries
    local arm64_dir="${symbol_dir}/out/arm64-v8a"
    if [ -f "${arm64_dir}/libwg-go.so.dbg" ]; then
        local size=$(du -h "${arm64_dir}/libwg-go.so.dbg" | cut -f1)
        echo -e "  ${GREEN}✓${NC} libwg-go.so.dbg (${size})"
    fi
    if [ -f "${arm64_dir}/libcharon.so.dbg" ]; then
        local size=$(du -h "${arm64_dir}/libcharon.so.dbg" | cut -f1)
        echo -e "  ${GREEN}✓${NC} libcharon.so.dbg (${size})"
    fi
    if [ -f "${arm64_dir}/libovpnutil.so.dbg" ]; then
        local size=$(du -h "${arm64_dir}/libovpnutil.so.dbg" | cut -f1)
        echo -e "  ${GREEN}✓${NC} libovpnutil.so.dbg (${size})"
    fi

    # Create zip with correct structure (ABI folders at root)
    echo "  Creating zip archive..."
    cd "${symbol_dir}/out"
    zip -r "${output_zip}" arm64-v8a/ armeabi-v7a/ x86/ x86_64/ > /dev/null 2>&1
    cd - > /dev/null

    local zip_size=$(du -h "${output_zip}" | cut -f1)
    echo -e "  ${GREEN}✓${NC} Created: ${output_zip} (${zip_size})"

    return 0
}

# Extract for mobile
extract_symbols "mobile"

# Extract for TV
extract_symbols "tv"

echo -e "\n${GREEN}✅ Symbol extraction complete!${NC}"
echo -e "\nSymbol files saved to: ${OUTPUT_DIR}"
echo -e "Upload these files to Google Play Console → App Bundle Explorer → Upload native debug symbols"
