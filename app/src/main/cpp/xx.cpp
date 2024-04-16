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

void *orig_sub_E32B0 = NULL;
void *stub_sub_E32B0 = NULL;

int64_t proxy_sub_E32B0(int64_t a1, int a2, int64_t a3){
    // 执行 stack 清理（不可省略），只需调用一次
    SHADOWHOOK_STACK_SCOPE();
    __android_log_print(ANDROID_LOG_INFO, TAG, "sub_E32B0 hooked args: 0x%jx, %d, 0x%jx\r\narg1:",
                        a1, a2, a3);
    print_bytes((void*)a1, 32);
    int64_t ret = SHADOWHOOK_CALL_PREV(proxy_sub_E32B0, a1, a2, a3);
    __android_log_print(ANDROID_LOG_INFO, TAG, "sub_E32B0 hooked args: 0x%jx, %d, 0x%jx\r\narg1:",
                        a1, a2, a3);
    print_bytes((void*)a1, 32);
    __android_log_print(ANDROID_LOG_INFO, TAG, "ret:");
    print_bytes((void*)ret, 32);
    return ret;
}

void do_hook() {
    // https://github.com/jmpews/Dobby/blob/a5135558f8ec50b885c779cb66fc7f15a02c869d/source/Backend/UserMode/PlatformUtil/Linux/ProcessRuntime.cc
    FILE *fp = fopen("/proc/self/maps", "r");
    if (fp == nullptr)
        return;
    while (!feof(fp)) {
        char line_buffer[LINE_MAX + 1];
        fgets(line_buffer, LINE_MAX, fp);
        // ignore the rest of characters
        if (strlen(line_buffer) == LINE_MAX && line_buffer[LINE_MAX] != '\n') {
            // Entry not describing executable data. Skip to end of line to set up
            // reading the next entry.
            int c;
            do {
                c = getc(fp);
            } while ((c != EOF) && (c != '\n'));
            if (c == EOF)
                break;
        }

        addr_t region_start, region_end;
        addr_t region_offset;
        char permissions[5] = {'\0'}; // Ensure NUL-terminated string.
        uint8_t dev_major = 0;
        uint8_t dev_minor = 0;
        long inode = 0;
        int path_index = 0;

        // Sample format from man 5 proc:
        //
        // address           perms offset  dev   inode   pathname
        // 08048000-08056000 r-xp 00000000 03:0c 64593   /usr/sbin/gpm
        //
        // The final %n term captures the offset in the input string, which is used
        // to determine the path name. It *does not* increment the return value.
        // Refer to man 3 sscanf for details.
        if (strstr(line_buffer, "SomeSo") == NULL) continue;
        if (sscanf(line_buffer,
                   "%" PRIxPTR "-%" PRIxPTR " %4c "
                   "%" PRIxPTR " %hhx:%hhx %ld %n",
                   &region_start, &region_end, permissions, &region_offset, &dev_major, &dev_minor,
                   &inode,
                   &path_index) < 7) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "/proc/self/maps parse failed!");
            fclose(fp);
            return;
        }

        // check header section permission
        if (strcmp(permissions, "r--p") != 0 && strcmp(permissions, "r-xp") != 0)
            continue;
        char *path_buffer = line_buffer + path_index;
        if (*path_buffer == 0 || *path_buffer == '\n' || *path_buffer == '[')
            continue;

        // strip
        if (path_buffer[strlen(path_buffer) - 1] == '\n') {
            path_buffer[strlen(path_buffer) - 1] = 0;
        }

        __android_log_print(ANDROID_LOG_INFO, TAG, "/proc/self/maps find  %s", path_buffer);
        __android_log_print(ANDROID_LOG_INFO, TAG, "libZaCipher.so base at %p",
                            (void *) region_start);
        fclose(fp);

        int64_t sub_E32B0_addr = (int64_t) region_start + 0x00000000000E32B0;
        stub_sub_E32B0 = shadowhook_hook_func_addr(
                (void*) sub_E32B0_addr,
                (void *) proxy_sub_E32B0,
                (void **)&orig_sub_E32B0
        );
        if(stub_sub_E32B0 == NULL)
        {
            int err_num = shadowhook_get_errno();
            const char *err_msg = shadowhook_to_errmsg(err_num);
            __android_log_print(ANDROID_LOG_INFO, TAG, "hook sub_E32B0 error %d - %s", err_num, err_msg);
        }
    }
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