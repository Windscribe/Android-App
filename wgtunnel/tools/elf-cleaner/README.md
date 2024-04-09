# termux-elf-cleaner

Utility for Android ELF files to remove unused parts that the linker warns about.

## Description

When loading ELF files, the Android linker warns about unsupported dynamic section entries with warnings such as:

    WARNING: linker: /data/data/org.kost.nmap.android.networkmapper/bin/nmap: unused DT entry: type 0x6ffffffe arg 0x8a7d4
    WARNING: linker: /data/data/org.kost.nmap.android.networkmapper/bin/nmap: unused DT entry: type 0x6fffffff arg 0x3

This utility strips away the following dynamic section entries:

- `DT_RPATH` - not supported in any Android version.
- `DT_GNU_HASH` - supported from Android 6.0.
- `DT_RUNPATH` - supported from Android 7.0.
- `DT_VERDEF` - supported from Android 6.0.
- `DT_VERDEFNUM` - supported from Android 6.0.
- `DT_VERNEEDED` - supported from Android 6.0.
- `DT_VERNEEDNUM` - supported from Android 6.0.
- `DT_VERSYM` - supported from Android 6.0.
- `DT_AARCH64_BTI_PLT` - supported from Android 12.
- `DT_AARCH64_PAC_PLT` - supported from Android 12.
- `DT_AARCH64_VARIANT_PCS` - supported from Android 12.

It also removes the three ELF sections of type:

- `SHT_GNU_verdef`
- `SHT_GNU_verneed`
- `SHT_GNU_versym`

And makes sure that the alignment of a TLS segment is at least 32 (for 32bit arches) or 64 (for 64bit arches), to prevent errors similar to:

```
error: "valgrind": executable's TLS segment is underaligned: alignment is 8, needs to be at least 64 for ARM64 Bionic
```

## Usage

```
Usage: termux-elf-cleaner [OPTION-OR-FILENAME]...

Processes ELF files to remove unsupported section types and
dynamic section entries which the Android linker warns about.

Options:

--api-level NN        choose target api level, i.e. 21, 24, ..
--dry-run             print info but but do not remove entries
--quiet               do not print info about removed entries
--help                display this help and exit
--version             output version information and exit
```

## License

SPDX-License-Identifier: [GPL-3.0-or-later](https://spdx.org/licenses/GPL-3.0-or-later.html)

## See also

* [Bionic's related docs](https://android.googlesource.com/platform/bionic/+/refs/heads/master/android-changes-for-ndk-developers.md)
