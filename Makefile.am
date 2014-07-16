
pkgdata_DATA = device_filter.xml

jardir = $(prefix)/jar
jar_DATA = $(UTILS_JAR) $(ANTTASKS_JAR)

UTILS_JAR = sigrok-androidutils.jar

ANTTASKS_JAR = ant/sigrok-androidutils-anttasks.jar

ANTFLAGS = -Dandroid.sdk=$(ANDROID_SDK) -Dandroid.platform=$(ANDROID_PLATFORM)

all : $(UTILS_JAR) $(ANTTASKS_JAR)

$(UTILS_JAR): always
	@ant $(ANTFLAGS) utils

$(ANTTASKS_JAR): always
	@ant anttasks

.PHONY: always

always:

