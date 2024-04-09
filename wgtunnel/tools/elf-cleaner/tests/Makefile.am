# Makefile for termux-elf-cleaner regression tests.

# Copyright 2022 Termux

# This file is part of termux-elf-cleaner.

# termux-elf-cleaner is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 3 of the
# License, or (at your option) any later version.

# termux-elf-cleaner is distributed in the hope that it will be
# useful, but WITHOUT ANY WARRANTY; without even the implied warranty
# of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.

# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

TESTSUITE_FRAGMENTS = api-21.at api-24.at tls-alignment.at elf-cleaner.at

EXTRA_DIST = $(TESTSUITE_AT) testsuite package.m4

DISTCLEANFILES       = atconfig $(check_SCRIPTS)
MAINTAINERCLEANFILES = Makefile.in $(TESTSUITE)
CLEANFILES =

## ------------ ##
## package.m4.  ##
## ------------ ##

$(builddir)/package.m4: $(top_srcdir)/configure.ac
	{                                      \
	  echo '# Signature of the current package.'; \
	  echo 'm4_define([AT_PACKAGE_NAME],      [@PACKAGE_NAME@])'; \
	  echo 'm4_define([AT_PACKAGE_VERSION],   [@PACKAGE_VERSION@])'; \
	  echo 'm4_define([AT_PACKAGE_STRING],    [@PACKAGE_STRING@])'; \
	  echo 'm4_define([AT_PACKAGE_BUGREPORT], [@PACKAGE_BUGREPORT@])'; \
	} >$(builddir)/package.m4

## ------------ ##
## Test suite.  ##
## ------------ ##

TESTSUITE_AT = elf-cleaner.at

TESTSUITE = $(builddir)/testsuite

AUTOTEST = $(AUTOM4TE) --language=autotest
$(TESTSUITE): package.m4 $(TESTSUITE_FRAGMENTS) $(TESTSUITE_AT)
	$(AUTOTEST) -I$(srcdir) $(TESTSUITE_AT) -o $@.tmp
	mv $@.tmp $@

clean-local:
	test ! -f $(TESTSUITE) || $(SHELL) $(TESTSUITE) --clean

check-local: $(TESTSUITE) atlocal
	$(SHELL) $(TESTSUITE) $(TESTSUITEFLAGS)

check-full:
	FULL_TEST=1 $(MAKE) check
