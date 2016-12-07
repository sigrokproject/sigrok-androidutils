##
## This file is part of the sigrok-androidutils project.
##
## Copyright (C) 2014 Marcus Comstedt <marcus@mc.pp.se>
##
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <http://www.gnu.org/licenses/>.
##

lib_LTLIBRARIES = libsigrokandroidutils.la

libsigrokandroidutils_la_CXXFLAGS = \
	-fno-exceptions

libsigrokandroidutils_la_SOURCES = \
	lib/jvm_glue.cpp \
	lib/envsetup.cpp

pkgconfigdir = $(libdir)/pkgconfig
pkgconfig_DATA = lib/libsigrokandroidutils.pc

library_includedir = $(includedir)/libsigrokandroidutils
library_include_HEADERS = lib/libsigrokandroidutils.h
noinst_HEADERS = lib/libsigrokandroidutils-internal.h

dist_pkgdata_DATA = device_filter.xml

jardir = $(prefix)/jar
jar_DATA = $(UTILS_JAR) $(ANTTASKS_JAR)

mvndir = $(prefix)/.m2/repository/org/sigrok/sigrok-core-android/$(BINDINGS_VERSION)
mvn_DATA = $(SIGROK_CORE_AAR) $(SIGROK_CORE_POM)

UTILS_JAR = sigrok-androidutils.jar

ANTTASKS_JAR = ant/sigrok-androidutils-anttasks.jar

BINDINGS_VERSION = $(LIBSIGROKCXX_VERSION)

SIGROK_CORE_AAR = sigrok-core-android-$(BINDINGS_VERSION).aar
SIGROK_CORE_POM = sigrok-core-android-$(BINDINGS_VERSION).pom

ANTFLAGS = -Dandroid.sdk=$(ANDROID_SDK) -Dandroid.platform=$(ANDROID_PLATFORM) \
	-Dprefix=$(prefix) -Dbindings.version=$(BINDINGS_VERSION)

EXTRA_DIST = build.xml src ant/src ant/resources

$(UTILS_JAR): always
	$(AM_V_GEN)ant -S -q $(ANTFLAGS) utils

$(ANTTASKS_JAR): always
	$(AM_V_GEN)ant -S -q anttasks

$(SIGROK_CORE_AAR): always $(UTILS_JAR) $(ANTTASKS_JAR) $(lib_LTLIBRARIES)
	$(AM_V_GEN)ant -S -q $(ANTFLAGS) aar

$(SIGROK_CORE_POM): always
	$(AM_V_GEN)ant -S -q $(ANTFLAGS) pom

clean-local:
	-rm -rf build ant/build jni
	-rm -f $(UTILS_JAR) $(ANTTASKS_JAR)
	-rm -f $(SIGROK_CORE_AAR) $(SIGROK_CORE_POM) classes.jar

.PHONY: always

always:

