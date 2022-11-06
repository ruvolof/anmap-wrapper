#include <android/log.h>
#include <fcntl.h>
#include <iostream>
#include <jni.h>
#include <string>
#include <unistd.h>
#include "nmap-7.93/nmap.h"

#define LOG_TAG "ANMAPWRAPPER_CUSTOM_LOG_NDK"
#define NMAP_END_TAG "NMAP_END"

class ExitTrapException : public std::runtime_error {
    public: explicit ExitTrapException(int status) : std::runtime_error(
            "Intercepted exit call with status " + std::to_string(status)) {}
};

extern "C" void exit(int status) {
    throw ExitTrapException(status);
}

extern void set_program_name(const char* name);

void flush_output_buffers() {
    std::cout << std::flush;
    std::cerr << std::flush;
}

void execNmapScan(int nmap_argc, char* nmap_argv[], char* output_path) {
    int saved_stdout = dup(STDOUT_FILENO);
    int saved_stderr = dup(STDERR_FILENO);
    int fifo_fd = open(output_path, O_WRONLY);
    dup2(fifo_fd, STDOUT_FILENO);
    dup2(fifo_fd, STDERR_FILENO);
    close(fifo_fd);

    set_program_name(nmap_argv[0]);
    try {
        nmap_main(nmap_argc, nmap_argv);
    } catch (ExitTrapException& e) {
        __android_log_print(ANDROID_LOG_DEBUG,
                            LOG_TAG,
                            "Exit call intercepted. Flushing buffers.");
        flush_output_buffers();
    }

    dup2(saved_stdout, STDOUT_FILENO);
    dup2(saved_stderr, STDERR_FILENO);
    close(saved_stdout);
    close(saved_stderr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_werebug_anmapwrapper_MainActivity_startScan(
        JNIEnv* env, jobject, jobjectArray argv, jstring fifo_path) {
    jsize nmap_argc = env->GetArrayLength(argv);
    char* nmap_argv[nmap_argc];
    for (int i = 0; i < nmap_argc; i++) {
        auto arg = (jstring)env->GetObjectArrayElement(argv, i);
        nmap_argv[i] = (char*)env->GetStringUTFChars(arg, 0);
    }
    auto output_path = (char*)env->GetStringUTFChars(fifo_path, 0);
    execNmapScan(nmap_argc, nmap_argv, output_path);
}
