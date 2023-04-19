#include "nmap.h"

extern void set_program_name(const char* name);

int main(int argc, char *argv[]) {
    set_program_name(argv[0]);
    return nmap_main(argc, argv);
}
