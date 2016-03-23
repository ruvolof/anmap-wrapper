/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "nmap.h"

#define MAX_WRAPPER_ARGS 50

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jnii/project/src/com/example/hellojni/ANmap.java
 */

extern void set_program_name(const char *name);

extern "C" {

jstring Java_org_nmap_anmap_ANmap_nmapWrapper(JNIEnv *env, jobject thiz, jobjectArray argv) {

    // Converting arguments from Java to C
    int my_argc = env->GetArrayLength(argv);
    char* my_argv[MAX_WRAPPER_ARGS];

    for (int i = 0; i < MAX_WRAPPER_ARGS; i++) {
        my_argv[i] = NULL;
    }

    for (int i = 0; i < my_argc; i++) {
        jstring s = (jstring) (env->GetObjectArrayElement(argv, i));
        my_argv[i] = strdup(env->GetStringUTFChars(s, 0));
    }

    // Required for a correct bootstrap of nmap
    set_program_name(my_argv[0]);

    int fd[2];
    pipe(fd);

    pid_t pid = fork();

    if (pid == 0) {
        // Redirecting output
        close(fd[0]);
        dup2(fd[1], STDOUT_FILENO);
        dup2(fd[1], STDERR_FILENO);
        close(fd[1]);

        // Running nmap
        nmap_main(my_argc, my_argv);

        // Freeing allocated memory
        for (int i = 0; i < MAX_WRAPPER_ARGS; ++i) {
            if (my_argv[i] != NULL) {
                free(my_argv[i]);
            }
        }

        exit(0);
    }

    // Closing write end of pipe
    close(fd[1]);

    // Reading nmap output
    char text[15000];
    int p = 0;
    while ((read(fd[0], text+p, 1)) == 1) {
        p++;
    }
    text[p] = '\0';
    close(fd[0]);

    // Returning output to ANmap
    return env->NewStringUTF(text);
}

}


