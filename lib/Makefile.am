lib_LTLIBRARIES = libsigrokandroidutils.la

libsigrokandroidutils_la_CXXFLAGS = -fno-exceptions

libsigrokandroidutils_la_SOURCES = \
	jvm_glue.cpp \
	envsetup.cpp

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = libsigrokandroidutils.pc

library_includedir = $(includedir)/libsigrokandroidutils
library_include_HEADERS = libsigrokandroidutils.h
noinst_HEADERS = libsigrokandroidutils-internal.h
