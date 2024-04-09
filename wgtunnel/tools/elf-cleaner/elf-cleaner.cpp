/* termux-elf-cleaner

Copyright (C) 2017 Fredrik Fornwall
Copyright (C) 2019-2022 Termux

This file is part of termux-elf-cleaner.

termux-elf-cleaner is free software: you can redistribute it and/or
modify it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

termux-elf-cleaner is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with termux-elf-cleaner.  If not, see
<https://www.gnu.org/licenses/>.  */

#include <algorithm>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#include "arghandling.h"

// Include a local elf.h copy as not all platforms have it.
#include "elf.h"

#define DT_GNU_HASH 0x6ffffef5
#define DT_VERSYM 0x6ffffff0
#define DT_FLAGS_1 0x6ffffffb
#define DT_VERNEEDED 0x6ffffffe
#define DT_VERNEEDNUM 0x6fffffff

#define DT_AARCH64_BTI_PLT 0x70000001
#define DT_AARCH64_PAC_PLT 0x70000003
#define DT_AARCH64_VARIANT_PCS 0x70000005

#define DF_1_NOW	0x00000001	/* Set RTLD_NOW for this object.  */
#define DF_1_GLOBAL	0x00000002	/* Set RTLD_GLOBAL for this object.  */
#define DF_1_NODELETE	0x00000008	/* Set RTLD_NODELETE for this object.*/

/* Default to api level 21 unless arg --api-level given  */
uint8_t supported_dt_flags_1 = (DF_1_NOW | DF_1_GLOBAL);
int api_level = 21;

bool dry_run = false;
bool quiet = false;

static char const *const usage_message[] =
{ "\
\n\
Processes ELF files to remove unsupported section types and\n\
dynamic section entries which the Android linker warns about.\n\
\n\
Options:\n\
\n\
--api-level NN        choose target api level, i.e. 21, 24, ..\n\
--dry-run             print info but but do not remove entries\n\
--quiet               do not print info about removed entries\n\
--help                display this help and exit\n\
--version             output version information and exit\n"
};

template<typename ElfWord /*Elf{32_Word,64_Xword}*/,
	 typename ElfHeaderType /*Elf{32,64}_Ehdr*/,
	 typename ElfSectionHeaderType /*Elf{32,64}_Shdr*/,
	 typename ElfProgramHeaderType /*Elf{32,64}_Phdr*/,
	 typename ElfDynamicSectionEntryType /* Elf{32,64}_Dyn */>
bool process_elf(uint8_t* bytes, size_t elf_file_size, char const* file_name)
{
	if (sizeof(ElfSectionHeaderType) > elf_file_size) {
		fprintf(stderr, "%s: Elf header for '%s' would end at %zu but file size only %zu\n",
			PACKAGE_NAME, file_name, sizeof(ElfSectionHeaderType), elf_file_size);
		return false;
	}
	ElfHeaderType* elf_hdr = reinterpret_cast<ElfHeaderType*>(bytes);

	bool is_aarch64 = (elf_hdr->e_machine == 183); /* EM_AARCH64 */

	/* Check TLS segment alignment in program headers if api level is < 29 */
	size_t last_program_header_byte = elf_hdr->e_phoff + sizeof(ElfProgramHeaderType) * elf_hdr->e_phnum;
	if (last_program_header_byte > elf_file_size) {
		fprintf(stderr, "%s: Program header for '%s' would end at %zu but file size only %zu\n",
			PACKAGE_NAME, file_name, last_program_header_byte, elf_file_size);
		return false;
	}
	ElfProgramHeaderType* program_header_table = reinterpret_cast<ElfProgramHeaderType*>(bytes + elf_hdr->e_phoff);

	size_t tls_min_alignment = sizeof(ElfWord)*8;
	/* Iterate over program headers */
	for (unsigned int i = 1; i < elf_hdr->e_phnum; i++) {
		ElfProgramHeaderType* program_header_entry = program_header_table + i;
		if (program_header_entry->p_type == PT_TLS &&
		    program_header_entry->p_align < tls_min_alignment) {
			if (!quiet)
				printf("%s: Changing TLS alignment for '%s' to %u, instead of %u\n",
				       PACKAGE_NAME, file_name,
				       (unsigned int)tls_min_alignment,
				       (unsigned int)program_header_entry->p_align);
			if (!dry_run)
				program_header_entry->p_align = tls_min_alignment;
		}
	}

	size_t last_section_header_byte = elf_hdr->e_shoff + sizeof(ElfSectionHeaderType) * elf_hdr->e_shnum;
	if (last_section_header_byte > elf_file_size) {
		fprintf(stderr, "%s: Section header for '%s' would end at %zu but file size only %zu\n",
			PACKAGE_NAME, file_name, last_section_header_byte, elf_file_size);
		return false;
	}
	ElfSectionHeaderType* section_header_table = reinterpret_cast<ElfSectionHeaderType*>(bytes + elf_hdr->e_shoff);

	/* Iterate over section headers */
	for (unsigned int i = 1; i < elf_hdr->e_shnum; i++) {
		ElfSectionHeaderType* section_header_entry = section_header_table + i;
		if (section_header_entry->sh_type == SHT_DYNAMIC) {
			size_t const last_dynamic_section_byte = section_header_entry->sh_offset + section_header_entry->sh_size;
			if (last_dynamic_section_byte > elf_file_size) {
				fprintf(stderr, "%s: Dynamic section for '%s' would end at %zu but file size only %zu\n",
					PACKAGE_NAME, file_name, last_dynamic_section_byte, elf_file_size);
				return false;
			}

			size_t const dynamic_section_entries = section_header_entry->sh_size / sizeof(ElfDynamicSectionEntryType);
			ElfDynamicSectionEntryType* const dynamic_section =
				reinterpret_cast<ElfDynamicSectionEntryType*>(bytes + section_header_entry->sh_offset);

			unsigned int last_nonnull_entry_idx = 0;
			for (unsigned int j = dynamic_section_entries - 1; j > 0; j--) {
				ElfDynamicSectionEntryType* dynamic_section_entry = dynamic_section + j;
				if (dynamic_section_entry->d_tag != DT_NULL) {
					last_nonnull_entry_idx = j;
					break;
				}
			}
			for (unsigned int j = 0; j < dynamic_section_entries; j++) {
				ElfDynamicSectionEntryType* dynamic_section_entry = dynamic_section + j;

				char const* removed_name = nullptr;
				switch (dynamic_section_entry->d_tag) {
					case DT_GNU_HASH: if (api_level < 23) removed_name = "DT_GNU_HASH"; break;
					case DT_VERSYM: if (api_level < 23) removed_name = "DT_VERSYM"; break;
					case DT_VERNEEDED: if (api_level < 23) removed_name = "DT_VERNEEDED"; break;
					case DT_VERNEEDNUM: if (api_level < 23) removed_name = "DT_VERNEEDNUM"; break;
					case DT_VERDEF: if (api_level < 23) removed_name = "DT_VERDEF"; break;
					case DT_VERDEFNUM: if (api_level < 23) removed_name = "DT_VERDEFNUM"; break;
					case DT_RPATH: removed_name = "DT_RPATH"; break;
					case DT_RUNPATH: if (api_level < 24) removed_name = "DT_RUNPATH"; break;
					case DT_AARCH64_BTI_PLT: if (is_aarch64 && api_level < 31) removed_name = "DT_AARCH64_BTI_PLT"; break;
					case DT_AARCH64_PAC_PLT: if (is_aarch64 && api_level < 31) removed_name = "DT_AARCH64_PAC_PLT"; break;
					case DT_AARCH64_VARIANT_PCS: if (is_aarch64 && api_level < 31) removed_name = "DT_AARCH64_VARIANT_PCS"; break;
				}
				if (removed_name != nullptr) {
					if (!quiet)
						printf("%s: Removing the %s dynamic section entry from '%s'\n",
						       PACKAGE_NAME, removed_name, file_name);
					// Tag the entry with DT_NULL and put it last:
					if (!dry_run)
						dynamic_section_entry->d_tag = DT_NULL;
					// Decrease j to process new entry index:
					std::swap(dynamic_section[j--], dynamic_section[last_nonnull_entry_idx--]);
				} else if (dynamic_section_entry->d_tag == DT_FLAGS_1) {
					// Remove unsupported DF_1_* flags to avoid linker warnings.
					decltype(dynamic_section_entry->d_un.d_val) orig_d_val =
						dynamic_section_entry->d_un.d_val;
					decltype(dynamic_section_entry->d_un.d_val) new_d_val =
						(orig_d_val & supported_dt_flags_1);
					if (new_d_val != orig_d_val) {
						if (!quiet)
							printf("%s: Replacing unsupported DF_1_* flags %llu with %llu in '%s'\n",
							       PACKAGE_NAME,
							       (unsigned long long) orig_d_val,
							       (unsigned long long) new_d_val,
							       file_name);
						if (!dry_run)
							dynamic_section_entry->d_un.d_val = new_d_val;
					}
				}
			}
		}
		else if (api_level < 23 &&
			 (section_header_entry->sh_type == SHT_GNU_verdef ||
			  section_header_entry->sh_type == SHT_GNU_verneed ||
			  section_header_entry->sh_type == SHT_GNU_versym)) {
			if (!quiet)
				printf("%s: Removing version section from '%s'\n",
				       PACKAGE_NAME, file_name);
			if (!dry_run)
				section_header_entry->sh_type = SHT_NULL;
		}
	}
	return true;
}

int parse_file(const char *file_name)
{
	int fd = open(file_name, O_RDWR);
	if (fd < 0) {
		char* error_message;
		if (asprintf(&error_message, "open(\"%s\")", file_name) == -1)
			error_message = (char*) "open()";
		perror(error_message);
		return 1;
	}

	struct stat st;
	if (fstat(fd, &st) < 0) {
		perror("fstat()");
		if (close(fd) != 0)
			perror("close()");
		return 1;
	}

	if (st.st_size < (long long) sizeof(Elf32_Ehdr)) {
		if (close(fd) != 0) {
			perror("close()");
			return 1;
		}
		return 0;
	}

	void* mem = mmap(0, st.st_size, PROT_READ | PROT_WRITE,
			 MAP_SHARED, fd, 0);
	if (mem == MAP_FAILED) {
		perror("mmap()");
		if (close(fd) != 0)
			perror("close()");
		return 1;
	}

	uint8_t* bytes = reinterpret_cast<uint8_t*>(mem);
	if (!(bytes[0] == 0x7F && bytes[1] == 'E' &&
	      bytes[2] == 'L' && bytes[3] == 'F')) {
		// Not the ELF magic number.
		munmap(mem, st.st_size);
		if (close(fd) != 0) {
			perror("close()");
			return 1;
		}
		return 0;
	}

	if (bytes[/*EI_DATA*/5] != 1) {
		fprintf(stderr, "%s: Not little endianness in '%s'\n",
			PACKAGE_NAME, file_name);
		munmap(mem, st.st_size);
		if (close(fd) != 0) {
			perror("close()");
			return 1;
		}
		return 0;
	}

	uint8_t const bit_value = bytes[/*EI_CLASS*/4];
	if (bit_value == 1) {
		if (!process_elf<Elf32_Word, Elf32_Ehdr, Elf32_Shdr, Elf32_Phdr,
		    Elf32_Dyn>(bytes, st.st_size, file_name)) {
			munmap(mem, st.st_size);
			if (close(fd) != 0)
				perror("close()");
			return 1;
		}
	} else if (bit_value == 2) {
		if (!process_elf<Elf64_Xword, Elf64_Ehdr, Elf64_Shdr,
		    Elf64_Phdr, Elf64_Dyn>(bytes, st.st_size, file_name)) {
			munmap(mem, st.st_size);
			if (close(fd) != 0)
				perror("close()");
			return 1;
		}
	} else {
		fprintf(stderr, "%s: Incorrect bit value %d in '%s'\n",
			PACKAGE_NAME, bit_value, file_name);
		munmap(mem, st.st_size);
		if (close(fd) != 0)
			perror("close()");
		return 1;
	}

	if (msync(mem, st.st_size, MS_SYNC) < 0) {
		perror("msync()");
		munmap(mem, st.st_size);
		if (close(fd) != 0)
			perror("close()");
		return 1;
	}

	munmap(mem, st.st_size);
	close(fd);
	return 0;
}

int main(int argc, char **argv)
{
	int skip_args = 0;
	if (argc == 1 || argmatch(argv, argc, "-help", "--help", 3, NULL, &skip_args)) {
		printf("Usage: %s [OPTION-OR-FILENAME]...\n", argv[0]);
		for (unsigned int i = 0; i < ARRAYELTS(usage_message); i++)
			fputs(usage_message[i], stdout);
		return 0;
	}

	if (argmatch(argv, argc, "-version", "--version", 3, NULL, &skip_args)) {
		printf("%s %s\n", PACKAGE_NAME, PACKAGE_VERSION);
		printf(("%s\n"
			"%s comes with ABSOLUTELY NO WARRANTY.\n"
			"You may redistribute copies of %s\n"
			"under the terms of the GNU General Public License.\n"
			"For more information about these matters, "
			"see the file named COPYING.\n"),
			COPYRIGHT, PACKAGE_NAME, PACKAGE_NAME);
		return 0;
	}

	argmatch(argv, argc, "-api-level", "--api-level", 3, &api_level, &skip_args);

	if (api_level >= 23) {
		// The supported DT_FLAGS_1 values as of Android 6.0.
		supported_dt_flags_1 = (DF_1_NOW | DF_1_GLOBAL | DF_1_NODELETE);
	}

	if (argmatch(argv, argc, "-dry-run", "--dry-run", 3, NULL, &skip_args))
		dry_run = true;

	if (argmatch(argv, argc, "-quiet", "--quiet", 3, NULL, &skip_args))
		quiet = true;

	for (int i = skip_args+1; i < argc; i++) {
		if (parse_file(argv[i]) != 0)
			return 1;
	}
	return 0;
}
