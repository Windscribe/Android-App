# SPDX-License-Identifier: Apache-2.0
#
# Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.

BUILDDIR ?= $(CURDIR)/build
DESTDIR ?= $(CURDIR)/out

NDK_GO_ARCH_MAP_x86 := 386
NDK_GO_ARCH_MAP_x86_64 := amd64
NDK_GO_ARCH_MAP_arm := arm
NDK_GO_ARCH_MAP_arm64 := arm64
NDK_GO_ARCH_MAP_mips := mipsx
NDK_GO_ARCH_MAP_mips64 := mips64x
CD_TAG := 1.5.4
CD_COMMIT := 69340d151e159d055745450f16c988fba9210634
comma := ,
CLANG_FLAGS := --target=$(TARGET) --sysroot=$(SYSROOT)
export CGO_CFLAGS := $(CLANG_FLAGS) $(subst -mthumb,-marm,$(CFLAGS))
export CGO_LDFLAGS := $(CLANG_FLAGS) $(patsubst -Wl$(comma)--build-id=%,-Wl$(comma)--build-id=none,$(LDFLAGS)) -Wl,-soname=libwg-go.so
export GOARCH := $(NDK_GO_ARCH_MAP_$(ANDROID_ARCH_NAME))
export GOOS := android
export CGO_ENABLED := 1

# Must match the `go` directive in go.mod. If it is lower, cmd/go silently
# switches to an unpatched toolchain from the proxy and the boottime patch below
# stops reaching the binary; GOTOOLCHAIN=local makes that a build error instead.
GO_VERSION := 1.25.14
GO_PLATFORM := $(shell uname -s | tr '[:upper:]' '[:lower:]')-$(NDK_GO_ARCH_MAP_$(shell uname -m))
GO_TARBALL := go$(GO_VERSION).$(GO_PLATFORM).tar.gz
GO_HASH_darwin-amd64 := b09087a67d5792a8b0fcbf74212d62560c94ac8a8fd750ff920d8cb5f1e20118
GO_HASH_darwin-arm64 := 5b26c0b6f308240fca2614fb02f622cfcc8c0cc3b69c78bba4845489a4590259
GO_HASH_linux-amd64 := a21ae5633a269bcd7e90cf767e48225633795e99d831742cbf3397064fee7712
export GOTOOLCHAIN := local

default: $(DESTDIR)/libwg-go.so

$(GRADLE_USER_HOME)/caches/golang/$(GO_TARBALL):
	mkdir -p "$(dir $@)"
	flock "$@.lock" -c ' \
	[ -f "$@" ] && exit 0; \
	curl -o "$@.tmp" "https://dl.google.com/go/$(GO_TARBALL)" && \
	echo "$(GO_HASH_$(GO_PLATFORM))  $@.tmp" | sha256sum -c && \
	mv "$@.tmp" "$@"'

$(BUILDDIR)/go-$(GO_VERSION)/.prepared: $(GRADLE_USER_HOME)/caches/golang/$(GO_TARBALL)
	mkdir -p "$(dir $@)"
	flock "$@.lock" -c ' \
	[ -f "$@" ] && exit 0; \
	tar -C "$(dir $@)" --strip-components=1 -xzf "$^" && \
	patch -p1 -f -N -r- -d "$(dir $@)" < goruntime-boottime-over-monotonic.diff && \
	touch "$@"'
# -w drops DWARF. -s is omitted deliberately: it also drops the ELF symbol table,
# without which a crash here is an unresolvable address and the SYMBOL_TABLE
# upload in mobile/ and tv/ has nothing to extract. The packaged .so is stripped
# by AGP anyway.
LD_FLAGS := -w \
  -X github.com/Control-D-Inc/ctrld/cmd/cli.version=$(CD_TAG) \
  -X github.com/Control-D-Inc/ctrld/cmd/cli.commit=$(CD_COMMIT) \
  -X golang.zx2c4.com/wireguard/ipc.socketDirectory=/data/data/$(ANDROID_PACKAGE_NAME)/cache/wireguard \
  -buildid=
# Set PATH for build
$(DESTDIR)/libwg-go.so: export PATH := $(BUILDDIR)/go-$(GO_VERSION)/bin/:$(PATH)
# Build rule
# Sources listed so a source-only edit does not leave the old library in place.
$(DESTDIR)/libwg-go.so: $(BUILDDIR)/go-$(GO_VERSION)/.prepared go.mod go.sum $(wildcard *.go) $(wildcard *.c)
	go build -tags linux -ldflags='$(LD_FLAGS)' -v -trimpath -buildvcs=false -o "$@" -buildmode c-shared

.DELETE_ON_ERROR:
