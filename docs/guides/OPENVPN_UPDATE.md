# OpenVPN Update & Custom Patch Guide

This comprehensive guide covers OpenVPN/OpenSSL updates and the UDP stuffing patch applied to this project.

## Table of Contents

1. [Overview](#overview)
2. [Current Versions](#current-versions)
3. [UDP Stuffing Patch Documentation](#udp-stuffing-patch-documentation)
4. [OpenVPN/OpenSSL Update Process](#openvpnopenssl-update-process)
5. [Testing & Verification](#testing--verification)
6. [Troubleshooting](#troubleshooting)
7. [Security & Maintenance](#security--maintenance)

---

## Overview

This project's OpenVPN module is based on [ics-openvpn](https://github.com/schwabe/ics-openvpn), an open-source OpenVPN client for Android maintained by Arne Schwabe. We periodically sync with their updates to get the latest OpenVPN and OpenSSL versions.

**⚠️ CRITICAL**: This project includes custom UDP stuffing patches that must be reapplied after every OpenVPN update.

### What is UDP Stuffing?

The UDP stuffing patch implements anti-DPI (Deep Packet Inspection) techniques to bypass censorship and traffic inspection:

1. **UDP Stuffing**: Sends 10-100 random packets (200-900 bytes each) before control reset packets
2. **TCP Split-Reset**: Splits TCP reset packets into small random-sized pieces (2-10 bytes) with delays

These techniques confuse DPI systems that look for specific OpenVPN traffic patterns.

---

## Current Versions

**As of October 2025**:
- **OpenSSL**: 3.6.0 (October 1, 2025)
- **OpenVPN**: 2.7 beta2
- **NDK**: 27.2.12479018
- **Last ics-openvpn Sync**: October 8, 2025 (commit 9aee9b5)
- **UDP Stuffing Patch**: ✅ Fully Applied

---

## UDP Stuffing Patch Documentation

### Command-Line Options

Enable these options in OpenVPN configuration:

```
# Enable UDP stuffing (sends random packets before reset)
udp-stuffing

# Enable TCP split-reset (splits reset packets into small pieces)
tcp-split-reset
```

Both options can be used together for maximum DPI evasion.

### Files Modified

The UDP stuffing patch modifies 6 OpenVPN source files:

#### 1. socket.h (Lines 670-719)
**Status**: ✅ Applied

**Location**: `openvpn/src/main/cpp/openvpn/src/openvpn/socket.h`

**Purpose**: Defines socket flags and UDP stuffing implementation

**Key Changes**:
- Added `SF_UDP_STUFFING` flag definition
- Added `SF_TCP_SPLITRESET` flag definition
- Implemented `link_socket_write_udp_stuffing()` function
- Sends 10-100 random packets (200-900 bytes) before actual packet

**Code Added**:
```c
// Socket flags (add to existing flags)
#define SF_UDP_STUFFING (1<<7)
#define SF_TCP_SPLITRESET (1<<8)

// UDP stuffing implementation
static inline int
link_socket_write_udp_stuffing(struct link_socket *sock,
                                struct buffer *buf,
                                struct link_socket_actual *to)
{
    // Send 10-100 random packets before actual packet
    uint8_t random_count;
    rand_bytes(&random_count, sizeof(random_count));
    int num_packets = (random_count % 91) + 10;

    for (int i = 0; i < num_packets; i++)
    {
        // Generate random packet size (200-900 bytes)
        uint16_t random_size;
        rand_bytes((uint8_t*)&random_size, sizeof(random_size));
        random_size = (random_size % 701) + 200;

        // Create and send random packet
        struct buffer random_buf = alloc_buf(random_size);
        rand_bytes(BPTR(&random_buf), random_size);
        random_buf.len = random_size;

        link_socket_write_udp(sock, &random_buf, to);
        free_buf(&random_buf);
    }

    // Send actual packet
    return link_socket_write_udp(sock, buf, to);
}
```

**Verification Command**:
```bash
grep -n "SF_UDP_STUFFING\|SF_TCP_SPLITRESET" openvpn/src/main/cpp/openvpn/src/openvpn/socket.h
```

---

#### 2. options.c (Lines 7717-7719)
**Status**: ✅ Applied

**Location**: `openvpn/src/main/cpp/openvpn/src/openvpn/options.c`

**Purpose**: Parse command-line options for UDP stuffing and TCP split-reset

**Key Changes**:
```c
else if (streq(p[0], "udp-stuffing"))
{
    VERIFY_PERMISSION(OPT_P_GENERAL);
    options->sockflags |= SF_UDP_STUFFING;
}
else if (streq(p[0], "tcp-split-reset"))
{
    VERIFY_PERMISSION(OPT_P_GENERAL);
    options->sockflags |= SF_TCP_SPLITRESET;
}
```

**Context**: Add this in the main options parsing loop, after line 7716.

**Verification Command**:
```bash
grep -n "udp-stuffing\|tcp-split-reset" openvpn/src/main/cpp/openvpn/src/openvpn/options.c
```

**Expected Output**: Should show lines ~7717-7719

---

#### 3. socket.c (After line 2448)
**Status**: ✅ Applied

**Location**: `openvpn/src/main/cpp/openvpn/src/openvpn/socket.c`

**Purpose**: Implement TCP split-reset logic in the TCP write function

**Function**: `link_socket_write_tcp()`

**Key Changes**: Add after line 2448 (after extracting opcode, before ASSERT):

```c
uint8_t opcode = *BPTR(buf) >> P_OPCODE_SHIFT;

ASSERT(buf_write_prepend(buf, &len, sizeof(len)));

// TCP SPLIT-RESET LOGIC - Add this block here
if (sock->sockflags & SF_TCP_SPLITRESET)
{
    if (opcode == P_CONTROL_HARD_RESET_CLIENT_V2
        || opcode == P_CONTROL_HARD_RESET_CLIENT_V3)
    {
        int size = 0;
        uint8_t split_piece_len, split_piece_len_cur;
        int left;

        // Generate random piece size (2-10 bytes)
        rand_bytes((uint8_t*)&split_piece_len, sizeof(split_piece_len));
        split_piece_len = (split_piece_len % 8) + 2;
        left = buf->len;

        // Set TCP_NODELAY for immediate sending
        socket_set_tcp_nodelay(sock->sd, 1);

        // Split and send packet in small pieces
        while (left) {
            split_piece_len_cur = (split_piece_len > left) ? left : split_piece_len;
            buf->len = split_piece_len_cur;
#ifdef _WIN32
            size += link_socket_write_win32(sock, buf, to);
#else
            size += link_socket_write_tcp_posix(sock, buf);
#endif
            buf->len = left;
            left -= split_piece_len_cur;
            buf_advance(buf, split_piece_len_cur);

            // 5ms delay between pieces
#ifdef _WIN32
            Sleep(5);
#else
            usleep(5000);
#endif
        }

        // Restore TCP_NODELAY setting
        if (!(sock->sockflags & SF_TCP_NODELAY)) {
            socket_set_tcp_nodelay(sock->sd, 0);
        }

        return size;
    }
}
```

**Exact Insertion Point**: Between lines 2448 and 2449, after:
```c
ASSERT(buf_write_prepend(buf, &len, sizeof(len)));
```

**Verification Command**:
```bash
grep -n "SF_TCP_SPLITRESET" openvpn/src/main/cpp/openvpn/src/openvpn/socket.c
```

**Expected Output**: Should show usage around line 2450-2485

---

#### 4. init.c (Line 3335)
**Status**: ✅ Applied

**Location**: `openvpn/src/main/cpp/openvpn/src/openvpn/init.c`

**Purpose**: Pass TCP split-reset flag to TLS options

**Function**: `do_init_crypto_tls()`

**Key Changes**: Add after line 3333 (after `to.data_epoch_supported`):

```c
to.data_epoch_supported = options->data_epoch_supported;
to.tcp_split_reset = !!(options->sockflags & SF_TCP_SPLITRESET && options->ce.proto != PROTO_UDP);
```

**Context**: This is in the function that initializes TLS options from command-line options.

**Verification Command**:
```bash
grep -n "tcp_split_reset" openvpn/src/main/cpp/openvpn/src/openvpn/init.c
```

**Expected Output**: Should show line ~3335

---

#### 5. ssl_common.h (Line 456)
**Status**: ✅ Applied

**Location**: `openvpn/src/main/cpp/openvpn/src/openvpn/ssl_common.h`

**Purpose**: Add TCP split-reset field to TLS options structure

**Struct**: `tls_options`

**Key Changes**: Add field in struct definition (around line 456):

```c
struct tls_options
{
    // ... existing fields ...
    bool dco_enabled;
    bool tcp_split_reset;  // <-- ADD THIS LINE

    /* ... rest of struct ... */
};
```

**Context**: Add after `dco_enabled` field and before any other fields.

**Verification Command**:
```bash
grep -n "bool tcp_split_reset" openvpn/src/main/cpp/openvpn/src/openvpn/ssl_common.h
```

**Expected Output**: Should show line ~456

---

#### 6. ssl.c (Lines 2809-2829)
**Status**: ✅ Applied

**Location**: `openvpn/src/main/cpp/openvpn/src/openvpn/ssl.c`

**Purpose**: Send multiple reset packets for UDP stuffing

**Function**: `tls_process_state()`

**Key Changes**: Add after line 2807 (after `dmsg(D_TLS_DEBUG, "Reliable -> TCP/UDP")`):

```c
write_control_auth(session, ks, &b, to_link_addr, opcode, CONTROL_SEND_ACK_MAX, true);
*to_link = b;
dmsg(D_TLS_DEBUG, "Reliable -> TCP/UDP");

// MULTIPLE RESET PACKETS - Add this block here
/* Send multiple reset packets for UDP stuffing */
if (session->opt->tcp_split_reset
    && (opcode == P_CONTROL_HARD_RESET_CLIENT_V2
        || opcode == P_CONTROL_HARD_RESET_CLIENT_V3))
{
    uint8_t num_extra_packets;
    rand_bytes(&num_extra_packets, sizeof(num_extra_packets));
    num_extra_packets = (num_extra_packets % 5) + 3;  // 3-7 packets

    for (int i = 0; i < num_extra_packets; i++)
    {
#ifdef _WIN32
        Sleep(5);
#else
        usleep(5000);
#endif
        struct buffer b2 = *buf;
        write_control_auth(session, ks, &b2, to_link_addr, opcode, CONTROL_SEND_ACK_MAX, true);
        *to_link = b2;
    }
}
```

**Context**: This sends 3-7 duplicate reset packets with 5ms delays for anti-DPI.

**Verification Command**:
```bash
grep -n "num_extra_packets" openvpn/src/main/cpp/openvpn/src/openvpn/ssl.c
```

**Expected Output**: Should show usage around lines 2814-2828

---

### Patch Summary Table

| File | Lines | Status | Purpose |
|------|-------|--------|---------|
| socket.h | 670-719 | ✅ Applied | Socket flags & UDP stuffing function |
| options.c | 7717-7719 | ✅ Applied | Command-line option parsing |
| socket.c | 2448+ | ✅ Applied | TCP split-reset implementation |
| init.c | 3335 | ✅ Applied | Flag passing to TLS options |
| ssl_common.h | 456 | ✅ Applied | TLS options struct field |
| ssl.c | 2809-2829 | ✅ Applied | Multiple reset packet sending |

---

## OpenVPN/OpenSSL Update Process

### Prerequisites

- Git installed
- Android Studio with SDK/NDK
- Terminal access
- Sufficient disk space (~2GB)

### Step 1: Backup Current State

**CRITICAL**: Always backup before updating.

```bash
cd /path/to/androidapp

# Backup entire cpp folder
cp -r openvpn/src/main/cpp openvpn/src/main/cpp.backup

# Backup patch documentation
cp UDP_STUFFING_PATCH_APPLIED.md UDP_STUFFING_PATCH_APPLIED.md.backup
cp apply_remaining_udp_patch.md apply_remaining_udp_patch.md.backup

# Create backup timestamp file
date > openvpn_backup_$(date +%Y%m%d_%H%M%S).txt
```

### Step 2: Clone ics-openvpn Repository

```bash
cd /path/to/temp/folder
git clone --depth 1 https://github.com/schwabe/ics-openvpn.git
cd ics-openvpn
```

### Step 3: Initialize Submodules

The cpp folder contains several git submodules:

```bash
git submodule update --init --recursive
```

This downloads:
- `main/src/main/cpp/openvpn` - OpenVPN source code
- `main/src/main/cpp/openssl` - OpenSSL source code
- `main/src/main/cpp/asio` - Asio library
- `main/src/main/cpp/fmt` - fmt library
- `main/src/main/cpp/lz4` - LZ4 compression
- `main/src/main/cpp/mbedtls` - mbedTLS
- `main/src/main/cpp/openvpn3` - OpenVPN 3 library

### Step 4: Check What Changed

**IMPORTANT**: Check if patched files changed:

```bash
# Check recent commits
git log --since="4 weeks ago" --oneline -- main/src/main/cpp

# Check specific patched files
git log --oneline -- main/src/main/cpp/openvpn/src/openvpn/socket.h
git log --oneline -- main/src/main/cpp/openvpn/src/openvpn/socket.c
git log --oneline -- main/src/main/cpp/openvpn/src/openvpn/options.c
git log --oneline -- main/src/main/cpp/openvpn/src/openvpn/init.c
git log --oneline -- main/src/main/cpp/openvpn/src/openvpn/ssl_common.h
git log --oneline -- main/src/main/cpp/openvpn/src/openvpn/ssl.c

# See detailed changes in a file
git log -p -- main/src/main/cpp/openvpn/src/openvpn/socket.c | head -200
```

**⚠️ WARNING**: If patched files changed significantly, manual patch adjustment may be needed.

### Step 5: Copy Updated cpp Folder

```bash
cd /path/to/androidapp

# Remove old cpp folder
rm -rf openvpn/src/main/cpp

# Copy new cpp folder
cp -r /path/to/temp/folder/ics-openvpn/main/src/main/cpp openvpn/src/main/
```

### Step 6: Verify New Versions

```bash
# Check OpenSSL version
cat openvpn/src/main/cpp/openssl/openssl.version

# Check OpenVPN version
grep "PRODUCT_VERSION" openvpn/src/main/cpp/openvpn/version.m4

# Example output:
# define([PRODUCT_VERSION], [2.7_beta2])
```

### Step 7: Reapply UDP Stuffing Patch

**⚠️ CRITICAL STEP**: Patches must be reapplied after updating.

#### 7.1: Check Patch Locations

Before reapplying, check if line numbers changed:

```bash
# Check socket.h (should see SF_* flags around line 670)
sed -n '650,750p' openvpn/src/main/cpp/openvpn/src/openvpn/socket.h | grep -n "SF_"

# Check socket.c (look for link_socket_write_tcp around line 2400)
grep -n "link_socket_write_tcp" openvpn/src/main/cpp/openvpn/src/openvpn/socket.c

# Check options.c (look for option parsing around line 7700)
sed -n '7700,7730p' openvpn/src/main/cpp/openvpn/src/openvpn/options.c | grep -n "streq"

# Check init.c (look for do_init_crypto_tls around line 3300)
grep -n "do_init_crypto_tls" openvpn/src/main/cpp/openvpn/src/openvpn/init.c

# Check ssl_common.h (look for tls_options struct around line 400)
grep -n "struct tls_options" openvpn/src/main/cpp/openvpn/src/openvpn/ssl_common.h

# Check ssl.c (look for tls_process_state around line 2700)
grep -n "tls_process_state" openvpn/src/main/cpp/openvpn/src/openvpn/ssl.c
```

#### 7.2: Reapply Each Patch

Follow the detailed instructions in the [UDP Stuffing Patch Documentation](#udp-stuffing-patch-documentation) section above.

**Method 1: Using Claude Code**
```
Ask Claude: "Reapply the UDP stuffing patch to the updated OpenVPN code.
Follow the exact instructions in .claude/openvpn-update-and-patch-guide.md
section 'UDP Stuffing Patch Documentation'."
```

**Method 2: Manual Application**
- Edit each file listed in the patch documentation
- Copy the exact code snippets provided
- Insert at the correct line numbers (adjust if code changed)
- Save each file

#### 7.3: Verify Patch Application

**CRITICAL**: Verify every change was applied:

```bash
# 1. Verify socket.h
grep -c "SF_UDP_STUFFING" openvpn/src/main/cpp/openvpn/src/openvpn/socket.h
# Expected: At least 1

grep -c "link_socket_write_udp_stuffing" openvpn/src/main/cpp/openvpn/src/openvpn/socket.h
# Expected: At least 1

# 2. Verify options.c
grep -c "udp-stuffing" openvpn/src/main/cpp/openvpn/src/openvpn/options.c
# Expected: 1

grep -c "tcp-split-reset" openvpn/src/main/cpp/openvpn/src/openvpn/options.c
# Expected: 1

# 3. Verify socket.c
grep -c "SF_TCP_SPLITRESET" openvpn/src/main/cpp/openvpn/src/openvpn/socket.c
# Expected: At least 1

grep -c "split_piece_len" openvpn/src/main/cpp/openvpn/src/openvpn/socket.c
# Expected: At least 2

# 4. Verify init.c
grep -c "tcp_split_reset" openvpn/src/main/cpp/openvpn/src/openvpn/init.c
# Expected: 1

# 5. Verify ssl_common.h
grep -c "bool tcp_split_reset" openvpn/src/main/cpp/openvpn/src/openvpn/ssl_common.h
# Expected: 1

# 6. Verify ssl.c
grep -c "num_extra_packets" openvpn/src/main/cpp/openvpn/src/openvpn/ssl.c
# Expected: At least 3
```

**All checks must pass before proceeding!**

### Step 8: Clean Build

```bash
# Clean CMake cache
rm -rf openvpn/.cxx

# Clean Gradle build
./gradlew :openvpn:clean

# Build OpenVPN module
./gradlew :openvpn:assembleSkeletonDebug
```

**Expected Output**: `BUILD SUCCESSFUL in Xs`

### Step 9: Build Full App

```bash
# Build mobile app
./gradlew :mobile:assembleGoogleDebug

# Or build TV app
./gradlew :tv:assembleGoogleDebug
```

**Expected Output**: `BUILD SUCCESSFUL`

### Step 10: Verify Versions in Binary

```bash
# Find the built library
find mobile/build -name "libopenvpn.so" -type f | head -1

# Set path variable for convenience
LIB_PATH="mobile/build/intermediates/merged_native_libs/googleDebug/mergeGoogleDebugNativeLibs/out/lib/arm64-v8a/libopenvpn.so"

# Check OpenSSL version
strings $LIB_PATH | grep "OpenSSL 3"
# Expected: OpenSSL 3.6.0 1 Oct 2025 (or newer)

# Check OpenVPN version
strings $LIB_PATH | grep "OpenVPN 2"
# Expected: OpenVPN 2.7_beta2 (or newer)
```

### Step 11: Update Documentation

```bash
# Update version numbers in this file
# Update OPENVPN_UPDATE_GUIDE.md
# Update project CLAUDE.md if needed
# Update UDP_STUFFING_PATCH_APPLIED.md status
```

---

## Testing & Verification

### 1. Compilation Testing

```bash
# Test clean build
./gradlew :openvpn:clean
./gradlew :openvpn:assembleSkeletonDebug

# Expected: BUILD SUCCESSFUL
# Expected warnings: SM2P256, atomic alignment (safe to ignore)
```

### 2. Integration Testing

```bash
# Test full app build
./gradlew :mobile:assembleGoogleDebug

# Expected: BUILD SUCCESSFUL
# Expected: No errors in console
```

### 3. Runtime Testing

Deploy to device and test:

#### Basic VPN Connection
```
1. Install app on device
2. Configure VPN connection
3. Connect to server
4. Verify data flows (ping, web browsing)
5. Check connection logs for errors
```

#### Protocol Testing
```
1. Test UDP protocol connection
2. Test TCP protocol connection
3. Verify automatic protocol switching
4. Test fallback behavior
```

#### Anti-DPI Testing
```
1. Add to config:
   udp-stuffing
   tcp-split-reset

2. Connect to VPN
3. Monitor connection logs
4. Verify no DPI blocking occurs
5. Test in restricted network (if available)
```

### 4. Patch Verification Testing

```bash
# Generate test config with anti-DPI options
cat > test_config.ovpn <<EOF
client
remote vpn.example.com 1194
proto udp
udp-stuffing
tcp-split-reset
# ... other config options ...
EOF

# Deploy and test
# Verify connection succeeds
# Check logs for UDP stuffing activity
```

---

## Troubleshooting

### Submodules Not Initialized

**Symptoms**:
- Empty directories in `cpp` folder
- Missing `openssl`, `openvpn` subdirectories

**Solution**:
```bash
cd /path/to/ics-openvpn
git submodule update --init --recursive
```

### CMake Configuration Errors

**Symptoms**:
- `CMake Error: Could not find OpenSSL`
- `openssl.cmake not found`

**Solution**:
```bash
# Verify critical files exist
ls -la openvpn/src/main/cpp/openssl/openssl.cmake
ls -la openvpn/src/main/cpp/openssl/openssl.version

# If missing, submodules weren't initialized
cd /path/to/ics-openvpn
git submodule update --init --recursive
# Copy cpp folder again

# Clean and rebuild
rm -rf openvpn/.cxx
./gradlew :openvpn:clean
./gradlew :openvpn:assembleSkeletonDebug
```

### NDK Not Found

**Symptoms**:
- `NDK at /path/to/ndk did not have a source.properties file`
- `No toolchains found in the NDK`

**Solution**:
```bash
# Check available NDKs
ls ~/Library/Android/sdk/ndk/

# Install required NDK via Android Studio:
# Tools → SDK Manager → SDK Tools → NDK (Side by side)
# Select version 27.2.12479018

# Or update build.gradle.kts:
# Change ndkVersion in openvpn/build.gradle.kts line 16
```

### Build Fails After Patch

**Symptoms**:
- Compilation errors in patched files
- Undefined references to `SF_UDP_STUFFING`
- Missing `tcp_split_reset` field

**Solution**:
```bash
# Check each patch was applied
# Run verification commands from Step 7.3

# If any fail, reapply that specific patch
# Common issues:
# - Wrong line numbers (code changed upstream)
# - Incorrect indentation
# - Missing bracket or semicolon

# Fix the issue and rebuild
./gradlew :openvpn:clean
./gradlew :openvpn:assembleSkeletonDebug
```

### Patch Line Numbers Changed

**Symptoms**:
- Cannot find exact code location mentioned in guide
- Surrounding code looks different

**Solution**:
```bash
# Find new location by searching for nearby code
# Example: Finding socket.c insertion point

# Search for the function
grep -n "link_socket_write_tcp" openvpn/src/main/cpp/openvpn/src/openvpn/socket.c

# Read the function (adjust line numbers based on grep output)
sed -n '2400,2500p' openvpn/src/main/cpp/openvpn/src/openvpn/socket.c

# Find where opcode is extracted (our insertion point)
# Look for: uint8_t opcode = *BPTR(buf) >> P_OPCODE_SHIFT;
# Insert our patch code after that line

# Manually adjust and apply patch
# Test compilation
./gradlew :openvpn:assembleSkeletonDebug
```

### Assembly Instruction Errors

**Symptoms**:
- `invalid instruction mnemonic 'vsm4rnds4'`
- Assembly errors in OpenSSL code

**Solution**:
This was fixed in recent ics-openvpn versions. If you encounter this:

```bash
# Verify you're using latest ics-openvpn
cd /path/to/ics-openvpn
git pull
git submodule update --recursive

# Copy cpp folder again
# Reapply patches
# Rebuild
```

### UDP Stuffing Not Working

**Symptoms**:
- VPN connects but anti-DPI doesn't work
- Still blocked by DPI systems
- No UDP stuffing in logs

**Solution**:
```bash
# 1. Verify patches applied
# Run all verification commands from Step 7.3
# All must return expected values

# 2. Verify options enabled in config
grep -i "udp-stuffing\|tcp-split-reset" /path/to/config.ovpn

# 3. Check OpenVPN logs
# Enable verbose logging: verb 4
# Look for UDP stuffing activity

# 4. Rebuild from scratch
rm -rf openvpn/.cxx
./gradlew :openvpn:clean
./gradlew :openvpn:assembleSkeletonDebug

# 5. Reinstall app on device
# Test again
```

### Memory Errors or Crashes

**Symptoms**:
- App crashes when connecting
- Segmentation faults in logs
- Random disconnects

**Solution**:
```bash
# Likely issue: Patch applied incorrectly

# 1. Check for syntax errors in patches
# Compile with verbose output
./gradlew :openvpn:assembleSkeletonDebug --stacktrace

# 2. Review each patch carefully
# Ensure no missing brackets, semicolons
# Check buffer allocations are correct

# 3. Compare with backup
diff -u openvpn/src/main/cpp.backup/openvpn/src/openvpn/socket.c \
        openvpn/src/main/cpp/openvpn/src/openvpn/socket.c

# 4. If uncertain, rollback and reapply
# See Rollback Procedure section
```

---

## Security & Maintenance

### Security Update Checklist

When updating for security reasons:

#### 1. Check CVE Coverage

```bash
# Visit OpenSSL security advisories
open https://www.openssl.org/news/secadv.html

# Check what CVEs are fixed in new version
# Document CVE numbers and severity
```

#### 2. Document Update

Create an update record:

```bash
# Create security update log
cat > security_update_$(date +%Y%m%d).md <<EOF
# Security Update - $(date +%Y-%m-%d)

## Previous Versions
- OpenSSL: [old version]
- OpenVPN: [old version]

## New Versions
- OpenSSL: [new version]
- OpenVPN: [new version]

## CVEs Addressed
- CVE-XXXX-XXXXX: [Description]
- CVE-XXXX-XXXXX: [Description]

## Testing Results
- Compilation: [PASS/FAIL]
- Integration: [PASS/FAIL]
- Runtime: [PASS/FAIL]
- Anti-DPI: [PASS/FAIL]

## Patch Status
- UDP Stuffing: [Applied/Not Applied]
- All verifications: [PASS/FAIL]

## Notes
[Any issues encountered or special considerations]
EOF
```

#### 3. Security Testing

After updating:

```bash
# 1. Verify TLS/SSL functionality
# - Test certificate validation
# - Test cipher negotiation
# - Test TLS version support

# 2. Test encryption/decryption
# - Transfer large files through VPN
# - Verify data integrity
# - Check for corruption

# 3. Memory safety
# - Run with Valgrind (if possible)
# - Monitor for memory leaks
# - Check crash logs

# 4. Anti-DPI still works
# - Test with udp-stuffing enabled
# - Test with tcp-split-reset enabled
# - Verify DPI evasion successful
```

### Update Frequency Recommendations

| Scenario | Action | Timeframe |
|----------|--------|-----------|
| Critical CVE (9.0+) | Immediate update | Within 24 hours |
| High CVE (7.0-8.9) | Urgent update | Within 1 week |
| Medium CVE (4.0-6.9) | Scheduled update | Within 1 month |
| Low CVE (<4.0) | Next regular update | Next cycle |
| Major version release | Evaluate and test | 1-2 weeks after release |
| Regular maintenance | Check for updates | Every 1-2 months |

### Monitoring

**Weekly**:
- Check ics-openvpn commits: https://github.com/schwabe/ics-openvpn/commits/master

**Monthly**:
- OpenSSL releases: https://www.openssl.org/news/
- OpenVPN releases: https://openvpn.net/community-downloads/

**Immediate**:
- Security mailing lists
- CVE databases: https://nvd.nist.gov/
- GitHub security advisories

### Rollback Procedure

If update causes critical issues:

#### 1. Restore Backup

```bash
cd /path/to/androidapp

# Remove problematic version
rm -rf openvpn/src/main/cpp

# Restore backup
cp -r openvpn/src/main/cpp.backup openvpn/src/main/cpp

# Restore documentation
cp UDP_STUFFING_PATCH_APPLIED.md.backup UDP_STUFFING_PATCH_APPLIED.md
cp apply_remaining_udp_patch.md.backup apply_remaining_udp_patch.md
```

#### 2. Clean Rebuild

```bash
# Clean everything
rm -rf openvpn/.cxx
./gradlew :openvpn:clean

# Rebuild
./gradlew :openvpn:assembleSkeletonDebug
./gradlew :mobile:assembleGoogleDebug
```

#### 3. Verify Rollback

```bash
# Check versions
strings mobile/build/intermediates/merged_native_libs/googleDebug/mergeGoogleDebugNativeLibs/out/lib/arm64-v8a/libopenvpn.so | grep "OpenSSL"

# Should show old version
# Test VPN functionality
# Verify patches still work
```

#### 4. Investigate & Document

```bash
# Create rollback report
cat > rollback_report_$(date +%Y%m%d).md <<EOF
# Rollback Report - $(date +%Y-%m-%d)

## Reason for Rollback
[Describe issue that required rollback]

## Version Rolled Back
- OpenSSL: [new version] → [old version]
- OpenVPN: [new version] → [old version]

## Issue Details
[Error messages, symptoms, logs]

## Next Steps
[Plan for addressing issue and retrying update]
EOF
```

### Best Practices

1. **Always backup before updating**
   - Backup takes 30 seconds, recovery takes hours

2. **Test in development first**
   - Never update production directly
   - Test all features thoroughly

3. **Document everything**
   - Update this guide with new line numbers
   - Note any issues encountered
   - Keep update logs

4. **Verify patches thoroughly**
   - Run all verification commands
   - Test anti-DPI functionality
   - Check logs for patch activity

5. **Monitor after deployment**
   - Watch crash reports
   - Check user feedback
   - Monitor connection success rates

6. **Keep backups for one release cycle**
   - Don't delete backups immediately
   - Keep until new version proven stable

---

## Quick Reference

### Patch Verification One-Liner

```bash
echo "Socket.h:" && grep -c "SF_UDP_STUFFING" openvpn/src/main/cpp/openvpn/src/openvpn/socket.h && \
echo "Options.c:" && grep -c "udp-stuffing" openvpn/src/main/cpp/openvpn/src/openvpn/options.c && \
echo "Socket.c:" && grep -c "SF_TCP_SPLITRESET" openvpn/src/main/cpp/openvpn/src/openvpn/socket.c && \
echo "Init.c:" && grep -c "tcp_split_reset" openvpn/src/main/cpp/openvpn/src/openvpn/init.c && \
echo "SSL_common.h:" && grep -c "bool tcp_split_reset" openvpn/src/main/cpp/openvpn/src/openvpn/ssl_common.h && \
echo "SSL.c:" && grep -c "num_extra_packets" openvpn/src/main/cpp/openvpn/src/openvpn/ssl.c
```

Expected output: All counts should be ≥1

### Clean Build One-Liner

```bash
rm -rf openvpn/.cxx && ./gradlew :openvpn:clean && ./gradlew :openvpn:assembleSkeletonDebug
```

### Version Check One-Liner

```bash
echo "OpenSSL:" && cat openvpn/src/main/cpp/openssl/openssl.version && \
echo "OpenVPN:" && grep "PRODUCT_VERSION" openvpn/src/main/cpp/openvpn/version.m4
```

---

## Support & Resources

### Documentation
- **This Guide**: `.claude/openvpn-update-and-patch-guide.md`
- **Root Guide**: `OPENVPN_UPDATE_GUIDE.md`
- **Patch Status**: `UDP_STUFFING_PATCH_APPLIED.md`
- **Patch Instructions**: `apply_remaining_udp_patch.md`
- **Architecture**: `CLAUDE.md`

### External Resources
- **ics-openvpn**: https://github.com/schwabe/ics-openvpn
- **OpenSSL**: https://www.openssl.org/
- **OpenVPN**: https://openvpn.net/
- **CVE Database**: https://nvd.nist.gov/

### Contact
- **ics-openvpn Issues**: https://github.com/schwabe/ics-openvpn/issues
- **OpenSSL Issues**: https://github.com/openssl/openssl/issues
- **OpenVPN Community**: https://community.openvpn.net/

---

**Last Updated**: October 23, 2025
**Document Version**: 3.0
**Status**: All patches applied and verified ✅