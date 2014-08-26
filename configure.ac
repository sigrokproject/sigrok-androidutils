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

AC_INIT([sigrok-androidutils], [0.1.0], [sigrok-devel@lists.sourceforge.net],
	[sigrok-androidutils], [http://www.sigrok.org])

AC_CONFIG_SRCDIR([src/org/sigrok/androidutils/UsbHelper.java])

AM_INIT_AUTOMAKE([1.11 -Wall -Werror subdir-objects])
m4_ifdef([AM_SILENT_RULES], [AM_SILENT_RULES([yes])])
m4_ifdef([AM_PROG_AR], [AM_PROG_AR])

AC_ARG_WITH([android-sdk],
	[AS_HELP_STRING([--with-android-sdk],
	  [specify path to Android SDK @<:@default=/usr/local/android@:>@])],
	[ANDROID_SDK="$withval"],
	[ANDROID_SDK=/usr/local/android])

AC_ARG_WITH([android-platform],
	[AS_HELP_STRING([--with-android-platform],
	  [select Android platform to build for @<:@default=android-14@:>@])],
	[ANDROID_PLATFORM="$withval"],
	[ANDROID_PLATFORM=android-14])

LT_INIT
AC_PROG_CXX
AM_PROG_CC_C_O

AC_SUBST([ANDROID_SDK])
AC_SUBST([ANDROID_PLATFORM])

AC_OUTPUT([Makefile lib/libsigrokandroidutils.pc])
