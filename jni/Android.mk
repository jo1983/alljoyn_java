LOCAL_PATH := $(call my-dir)
AJ_DIST := $(LOCAL_PATH)/../../mbus/build/android/arm/$(APP_OPTIM)/dist
AJ_SRC := $(LOCAL_PATH)/../../mbus/src

include $(CLEAR_VARS)

LOCAL_MODULE    := alljoyn_java
LOCAL_SRC_FILES := alljoyn_java.cc
LOCAL_CPP_EXTENSION := .cc
LOCAL_PRE_CXXFLAGS := -I$(AJ_DIST)/inc/stlport
LOCAL_C_INCLUDES := $(AJ_DIST)/inc $(AJ_SRC)
LOCAL_CFLAGS := -DQCC_OS_GROUP_POSIX -DQCC_OS_ANDROID -DQCC_CPU_ARM -DANDROID 
LOCAL_LDLIBS := \
	$(AJ_DIST)/lib/liballjoyn.a \
	$(AJ_DIST)/lib/libajdaemon.a \
	$(AJ_DIST)/lib/BundledDaemon.o \
	-L$(ANDROID_SRC)/out/target/product/generic/system/lib -lcrypto -llog

include $(BUILD_SHARED_LIBRARY)
