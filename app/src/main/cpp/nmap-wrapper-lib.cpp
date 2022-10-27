#include <android/log.h>
#include <cstdint>
#include <cstdio>
#include <iostream>
#include <jni.h>
#include <string>
#include <sys/wait.h>
#include <unistd.h>
#include "nmap-7.93/nmap.h"

#define LOG_TAG "ANMAPWRAPPER_CUSTOM_LOG_NDK"
#define NMAP_END_TAG "NMAP_END"

int pipes[2];

class ExitTrapException : public std::runtime_error {
    public: explicit ExitTrapException(int status) : std::runtime_error(
            "Intercepted exit call with status " + std::to_string(status)) {}
};

extern "C" void exit(int status) {
    throw ExitTrapException(status);
}

extern void set_program_name(const char* name);

void execNmapScan(int nmap_argc, char* nmap_argv[]) {
    pipe(pipes);
    pid_t pid = fork();
    if (pid == 0) {
        close(pipes[0]);
        dup2(pipes[1], STDOUT_FILENO);
        dup2(pipes[1], STDERR_FILENO);
        close(pipes[1]);

        set_program_name(nmap_argv[0]);
        try {
            nmap_main(nmap_argc, nmap_argv);
        } catch (ExitTrapException& e) {
            __android_log_print(ANDROID_LOG_DEBUG,
                                LOG_TAG,
                                "Exit call intercepted. Flushing buffers.");
            std::cout << std::flush;
            std::cerr << std::flush;
        }
        return;
    }
    close(pipes[1]);
}

extern "C" JNIEXPORT void JNICALL
Java_com_werebug_anmapwrapper_MainActivity_startScan(JNIEnv* env, jobject, jobjectArray argv) {
    jsize nmap_argc = env->GetArrayLength(argv);
    char* nmap_argv[nmap_argc];
    for (int i = 0; i < nmap_argc; i++) {
        auto arg = (jstring)env->GetObjectArrayElement(argv, i);
        nmap_argv[i] = (char*)env->GetStringUTFChars(arg, 0);
    }
    execNmapScan(nmap_argc, nmap_argv);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_werebug_anmapwrapper_MainActivity_readNmapOutputStream(JNIEnv *env, jobject) {
    char output[501];
    memset(output, '\0', 501);
    ssize_t read_count = read(pipes[0], &output, 500);
    if (read_count == 0 || read_count == -1) {
        strncpy(output, NMAP_END_TAG, strlen(NMAP_END_TAG));
        close(pipes[0]);
    }
    return env->NewStringUTF(output);
}
