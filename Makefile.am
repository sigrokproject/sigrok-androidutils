
SUBDIRS = lib

dist_pkgdata_DATA = device_filter.xml

jardir = $(prefix)/jar
jar_DATA = $(UTILS_JAR) $(ANTTASKS_JAR)

UTILS_JAR = sigrok-androidutils.jar

ANTTASKS_JAR = ant/sigrok-androidutils-anttasks.jar

ANTFLAGS = -Dandroid.sdk=$(ANDROID_SDK) -Dandroid.platform=$(ANDROID_PLATFORM)

EXTRA_DIST = build.xml src ant/src ant/resources

$(UTILS_JAR): always
	@ant $(ANTFLAGS) utils

$(ANTTASKS_JAR): always
	@ant anttasks

clean-local:
	-rm -rf build ant/build
	-rm -f $(UTILS_JAR) $(ANTTASKS_JAR)

.PHONY: always

always:

