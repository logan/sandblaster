LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sandblaster
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := sandblaster.cc
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
