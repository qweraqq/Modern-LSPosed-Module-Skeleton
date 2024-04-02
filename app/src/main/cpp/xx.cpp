#include <jni.h>
//#include <string>

#include <iostream>
#include <cstring>
#include <sstream>
#include <iomanip>
#include <unistd.h>
#include <unwind.h>
#include <cstdarg>
#include <sys/ptrace.h>
#include <sys/syscall.h>
#include <fstream>
#include <stdint.h>
#include <inttypes.h>
#include <dlfcn.h>
#include <android/log.h>
#include "shadowhook.h"

#define LINE_MAX 2048
#define TAG "LSP-DEMO-Native"
typedef uintptr_t addr_t;

// some helper function
void print_bytes(void *ptr, int size)
{
    char desc[size*2+1];
    memset(desc, 0, size*2+1);
    unsigned char *p = static_cast<unsigned char *>(ptr);
    int i;
    for (i=0; i<size; i++) {
        snprintf(desc+i*2, 3, "%02hhX", p[i]);
//        __android_log_print(ANDROID_LOG_INFO, TAG, "%02hhX ", p[i]);
    }
    __android_log_print(ANDROID_LOG_INFO, TAG, "%s ", desc);
}

void do_hook() {

}


jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "Start native hook when JNI onLoad");
    int shadowhook_init_result = shadowhook_init(SHADOWHOOK_MODE_SHARED, false);
    __android_log_print(ANDROID_LOG_INFO, TAG, "shadowhook init result %d", shadowhook_init_result);
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK) {
        do_hook();
        return JNI_VERSION_1_6;
    }

    return 0;
}