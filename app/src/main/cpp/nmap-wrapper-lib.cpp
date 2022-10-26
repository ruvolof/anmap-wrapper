#include <iostream>
#include <jni.h>
#include <string>
#include <stdio.h>
#include <unistd.h>
#include "nmap-7.93/nmap.h"

int pipes[2];

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
        nmap_main(nmap_argc, nmap_argv);

        exit(0);
    }
    close(pipes[1]);
}

extern "C" JNIEXPORT void JNICALL
Java_com_werebug_anmapwrapper_MainActivity_startScan(JNIEnv* env,
                                                     jobject,
                                                     jobjectArray argv) {
    int nmap_argc = env->GetArrayLength(argv);
    char* nmap_argv[nmap_argc];
    for (int i = 0; i < nmap_argc; i++) {
        jstring arg = (jstring)env->GetObjectArrayElement(argv, i);
        nmap_argv[i] = (char*)env->GetStringUTFChars(arg, 0);
    }
    execNmapScan(nmap_argc, nmap_argv);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_werebug_anmapwrapper_MainActivity_readNmapOutputStream(JNIEnv *env,
                                                                jobject) {
    char output[501];
    memset(output, '\0', 501);
    int read_count = read(pipes[0], &output, 500);
    if (read_count == 0) {
        strncpy(output, "NMAP_END\0", 9);
        close(pipes[0]);
    }
    return env->NewStringUTF(output);
}
