dnl  Autoconf script for termux-elf-cleaner
dnl To rebuild the 'configure' script from this, execute the command
dnl	autoconf
dnl in the directory containing this script.
dnl
dnl Copyright (C) 2022 Termux
dnl
dnl  This file is part of termux-elf-cleaner.
dnl
dnl  termux-elf-cleaner is free software: you can redistribute it
dnl  and/or modify it under the terms of the GNU General Public
dnl  License as published by the Free Software Foundation, either
dnl  version 3 of the License, or (at your option) any later version.
dnl
dnl  termux-elf-cleaner is distributed in the hope that it will be
dnl  useful, but WITHOUT ANY WARRANTY; without even the implied
dnl  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
dnl  See the GNU General Public License for more details.
dnl
dnl  You should have received a copy of the GNU General Public License
dnl  along with termux-elf-cleaner.  If not, see
dnl  <https://www.gnu.org/licenses/>.

AC_PREREQ([2.69])
AC_INIT([termux-elf-cleaner], [2.1.1], [support@termux.dev])

AM_INIT_AUTOMAKE([foreign])

AC_PROG_MAKE_SET
AC_PROG_CC
AC_PROG_CXX
AC_LANG(C++)

copyright="Copyright (C) 2022 Termux."
AC_DEFINE_UNQUOTED(COPYRIGHT, ["$copyright"],
  [Short copyright string for this version of termux-elf-cleaner.])

AC_CONFIG_TESTDIR(tests)
AM_MISSING_PROG([AUTOM4TE], [autom4te])

AC_CONFIG_FILES([Makefile tests/Makefile tests/atlocal])

AC_OUTPUT
