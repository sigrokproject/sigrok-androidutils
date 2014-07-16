AC_INIT([sigrok-androidutils], [0.1.0])

AC_CONFIG_SRCDIR([src/org/sigrok/androidutils/UsbHelper.java])
AM_INIT_AUTOMAKE

AC_ARG_WITH([android-sdk],
	[AS_HELP_STRING([--with-android-sdk],
	  [specify path to android SDK @<:@default=/usr/local/android@:>@])],
	[ANDROID_SDK="$withval"],
	[ANDROID_SDK=/usr/local/android])

AC_ARG_WITH([android-platform],
	[AS_HELP_STRING([--with-android-platform],
	  [select android platform to build for @<:@default=android-14@:>@])],
	[ANDROID_PLATFORM="$withval"],
	[ANDROID_PLATFORM=android-14])

AC_SUBST([ANDROID_SDK])
AC_SUBST([ANDROID_PLATFORM])

AC_OUTPUT([Makefile])
