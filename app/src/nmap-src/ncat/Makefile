# Makefile.in generated automatically by automake 1.4-p6 from Makefile.am

# Copyright (C) 1994, 1995-8, 1999, 2001 Free Software Foundation, Inc.
# This Makefile.in is free software; the Free Software Foundation
# gives unlimited permission to copy and/or distribute it,
# with or without modifications, as long as this notice is preserved.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY, to the extent permitted by law; without
# even the implied warranty of MERCHANTABILITY or FITNESS FOR A
# PARTICULAR PURPOSE.

# Ncat Makefile


SHELL = /bin/bash
top_srcdir = .

datarootdir = ${prefix}/share
libexecdir = ${exec_prefix}/libexec
datadir = ${datarootdir}
sysconfdir = ${prefix}/etc
sharedstatedir = ${prefix}/com
localstatedir = ${prefix}/var
libdir = ${exec_prefix}/lib
infodir = ${datarootdir}/info
includedir = ${prefix}/include
oldincludedir = /usr/include

top_builddir = .

pkgdatadir = $(datadir)/ncat

INSTALL = /usr/bin/install -c
transform = s,x,x,

prefix = /usr/local
exec_prefix = ${prefix}
bindir = ${exec_prefix}/bin
sbindir = ${exec_prefix}/sbin
mandir = ${datarootdir}/man
srcdir = .

NBASEDIR = ../nbase
NSOCKDIR = ../nsock/src
NSOCKLIB = $(NSOCKDIR)/libnsock.a
NBASELIB = $(NBASEDIR)/libnbase.a

CC = gcc
CPPFLAGS = -I$(top_srcdir)/../liblua  -I../libpcap
CFLAGS = -g -O2 -Wall
LDFLAGS = -L../libpcap 
LIBS = -ldl 
DEFS = -DHAVE_CONFIG_H -DNCAT_DATADIR="\"$(pkgdatadir)\""
# With GCC, add extra security checks to source code.
DEFS += -D_FORTIFY_SOURCE=2
INCLS = -I. -I.. -I../nsock/include/ -I$(NBASEDIR)
RM = rm -f
STRIP = /usr/bin/strip
OPENSSL_LIBS = 
HAVE_OPENSSL = 
PCAP_LIBS = -lpcap
HAVE_LUA = $(top_srcdir)/../liblua/liblua.a
LIBLUADIR = ../liblua
LUA_CFLAGS = -DLUA_USE_POSIX -DLUA_USE_DLOPEN


CPPFLAGS += $(DEFS) $(INCLS)

SHTOOL = ../shtool

# DESTDIR can be used by package maintainers to install Ncat under its 
# usual directory structure into a different tree.
DESTDIR = 

SRCS = ncat_main.c ncat_connect.c ncat_core.c ncat_posix.c ncat_listen.c ncat_proxy.c ncat_ssl.c base64.c http.c util.c sys_wrap.c
OBJS = ncat_main.o ncat_connect.o ncat_core.o ncat_posix.o ncat_listen.o ncat_proxy.o ncat_ssl.o base64.o http.o util.o sys_wrap.o
DATAFILES =

ifneq ($(HAVE_OPENSSL),)
SRCS += http_digest.c
OBJS += http_digest.o
DATAFILES = certs/ca-bundle.crt
endif

ifneq ($(HAVE_LUA),)
LUA_SRCS = ncat_lua.c
LUA_OBJS = ncat_lua.o
LUA_LIBS = $(top_srcdir)/../liblua/liblua.a -lm
LUA_CFLAGS += -DHAVE_LUA=1 -I../liblua
else
LUA_SRCS =
LUA_OBJS =
LUA_LIBS =
LUA_CFLAGS =
endif

SRCS += $(LUA_SRCS)
OBJS += $(LUA_OBJS)

TARGET = ncat
mkinstalldirs = $(SHELL) $(top_srcdir)/mkinstalldirs
CONFIG_HEADER = config.h
CONFIG_CLEAN_FILES = $(CONFIG_HEADER) config.cache config.log config.status

TEST_PROGS = test/addrset test/test-uri test/test-cmdline-split
ifneq ($(HAVE_OPENSSL),)
TEST_PROGS += test/test-wildcard
endif

all: $(TARGET)

$(LIBLUADIR)/liblua.a: build-lua

build-lua: $(LIBLUADIR)/Makefile
	@echo Compiling liblua; cd $(LIBLUADIR) && $(MAKE) liblua.a CC="$(CC)" MYCFLAGS="$(CFLAGS) $(LUA_CFLAGS)"


$(TARGET): $(top_srcdir)/../liblua/liblua.a $(OBJS) $(NSOCKLIB)
	$(CC) -o $@ $(CFLAGS) $(lua_cflags) $(LDFLAGS) $(OBJS) $(NSOCKLIB) $(NBASELIB) $(OPENSSL_LIBS) $(PCAP_LIBS) $(LUA_LIBS) $(LIBS)

%.o: %.c
	$(CC) $(CPPFLAGS) $(LUA_CFLAGS) $(CFLAGS) -c $< -o $@

Makefile: $(srcdir)/Makefile.in  $(top_builddir)/config.status
	cd $(top_builddir) \
	  && CONFIG_FILES=$@ CONFIG_HEADERS= $(SHELL) ./config.status

config.status: $(srcdir)/configure $(CONFIG_STATUS_DEPENDENCIES)
	$(SHELL) ./config.status --recheck

$(srcdir)/configure: $(srcdir)/configure.ac $(CONFIGURE_DEPENDENCIES)
	cd $(srcdir) && autoconf

config.h:
	@if test ! -f config.h; then \
		cd $(top_builddir) && CONFIG_FILES= CONFIG_HEADERS=config.h \
		        $(SHELL) ./config.status; \
	fi

test/addrset: test/addrset.o ncat_core.o sys_wrap.o util.o ncat_posix.o $(LUA_OBJS) $(top_srcdir)/../liblua/liblua.a
	$(CC) -o $@ $(CFLAGS) $(LDFLAGS) $^ $(NSOCKLIB) $(NBASELIB) $(OPENSSL_LIBS) $(PCAP_LIBS) $(LUA_LIBS) $(LIBS)

test/test-uri: test/test-uri.o base64.o http.o ncat_core.o  sys_wrap.o util.o ncat_posix.o $(LUA_OBJS) $(top_srcdir)/../liblua/liblua.a
	$(CC) -o $@ $(CFLAGS) $(LDFLAGS) $^ $(NSOCKLIB) $(NBASELIB) $(OPENSSL_LIBS) $(PCAP_LIBS) $(LUA_LIBS) $(LIBS)

test/test-cmdline-split: test/test-cmdline-split.o ncat_posix.o ncat_core.o sys_wrap.o util.o $(LUA_OBJS) $(top_srcdir)/../liblua/liblua.a
	$(CC) -o $@ $(CFLAGS) $(LDFLAGS) $^ $(NSOCKLIB) $(NBASELIB) $(OPENSSL_LIBS) $(PCAP_LIBS) $(LUA_LIBS) $(LIBS)

test/test-wildcard: test/test-wildcard.o ncat_core.o ncat_ssl.o sys_wrap.o util.o ncat_posix.o $(LUA_OBJS) $(top_srcdir)/../liblua/liblua.a
	$(CC) -o $@ $(CFLAGS) $(LDFLAGS) $^ $(NSOCKLIB) $(NBASELIB) $(OPENSSL_LIBS) $(PCAP_LIBS) $(LUA_LIBS) $(LIBS)

.PHONY: uninstall all clean distclean build-lua

../libnetutil/libnetutil.a: ../libnetutil/Makefile
	@echo Compiling libnetutil;
	cd ../libnetutil && $(MAKE)

$(NBASEDIR)/libnbase.a: $(NBASEDIR)/Makefile
	@echo Compiling libnbase;
	cd $(NBASEDIR) && $(MAKE)

$(NSOCKDIR)/libnsock.a: $(NSOCKDIR)/Makefile
	@echo Compiling libnsock;
	cd $(NSOCKDIR) && $(MAKE)

install: $(TARGET)
	@echo Installing Ncat;
	$(SHTOOL) mkdir -f -p -m 755 $(DESTDIR)$(bindir) $(DESTDIR)$(mandir)/man1
	$(INSTALL) -c -m 755 ncat $(DESTDIR)$(bindir)/ncat
	$(STRIP) -x $(DESTDIR)$(bindir)/ncat
	if [ -n "$(DATAFILES)" ]; then \
		$(SHTOOL) mkdir -f -p -m 755 $(DESTDIR)$(pkgdatadir); \
		$(INSTALL) -c -m 644 $(DATAFILES) $(DESTDIR)$(pkgdatadir)/; \
	fi
	$(INSTALL) -c -m 644 docs/$(TARGET).1 $(DESTDIR)$(mandir)/man1/$(TARGET).1

uninstall:
	@echo Uninstalling Ncat;
	$(RM) -f $(DESTDIR)$(bindir)/$(TARGET)
	$(RM) -f $(DESTDIR)$(mandir)/man1/$(TARGET).1
	$(RM) -rf $(DESTDIR)$(pkgdatadir)/

clean-ncat:
	$(RM) -f *.o test/*.o $(TARGET) $(TEST_PROGS)

clean: clean-ncat clean-lua

clean-lua:
	-cd $(LIBLUADIR) && $(MAKE) clean

distclean-lua:
	-cd $(LIBLUADIR) && $(MAKE) clean

distclean: clean distclean-lua
	-rm -f Makefile makefile.dep $(CONFIG_CLEAN_FILES)

TESTS = ./test-addrset.sh ./test-cmdline-split ./test-uri
ifneq ($(HAVE_OPENSSL),)
TESTS += ./test-wildcard
endif

check: $(TARGET) $(TEST_PROGS)
	cd test && ($(addsuffix &&,$(TESTS)) echo "All tests passed.")

# Tell versions [3.59,3.63) of GNU make to not export all variables.
# Otherwise a system limit (for SysV at least) may be exceeded.
.NOEXPORT:

makefile.dep:
	$(CC) -MM $(CPPFLAGS) $(SRCS) > $@
include makefile.dep

