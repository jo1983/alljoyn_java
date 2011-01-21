LOCAL_PATH := $(call my-dir)
MBUS_DIST := $(LOCAL_PATH)/../../mbus/build/android/arm/$(APP_OPTIM)/dist
MBUS_SRC := $(LOCAL_PATH)/../../mbus/src

include $(CLEAR_VARS)

LOCAL_MODULE    := alljoyn_java
LOCAL_SRC_FILES := alljoyn_java.cc
LOCAL_CPP_EXTENSION := .cc
LOCAL_PRE_CXXFLAGS := -I$(MBUS_DIST)/inc/stlport
LOCAL_C_INCLUDES := $(MBUS_DIST)/inc $(MBUS_SRC)
LOCAL_CFLAGS := -DQCC_OS_GROUP_POSIX -DQCC_OS_ANDROID -DQCC_CPU_ARM -DANDROID 
LOCAL_LDLIBS := $(MBUS_DIST)/lib/liballjoyn.a \
	-L$(ANDROID_SRC)/out/target/product/generic/system/lib -lcrypto -llog

include $(BUILD_SHARED_LIBRARY)
