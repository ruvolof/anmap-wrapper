#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "nmap.h"

int main(int argcc, char** argvv) {
    int argc = 1;
    char * s[] = {
            "nmap",
            "fs1.francescoruvolo.com"
    };

    int fd[2];
    pipe(fd);

    pid_t pid = fork();

    if (pid == 0) {
        close(fd[0]);
        dup2(fd[1], STDOUT_FILENO);
        close(fd[1]);
        nmap_main(argc, s);
        exit(0);
    }

    close(fd[1]);
    char text[5000];
    int p = 0;
    while ((read(fd[0], text+p, 1)) == 1) {
        p++;
    }
    text[p] = '\0';
    close(fd[0]);
    
    printf("%s\n", text);

    return 1;
}

