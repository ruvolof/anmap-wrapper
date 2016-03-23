#
# nsock regression test suite
# Same license as nmap -- see https://nmap.org/book/man-legal.html
##

NBASEDIR=/home/francesco/Dropbox/Informatica/Programmazione/StudioProjects/ANmap/app/src/nmap-src/nbase
NSOCKLIB=../src/libnsock.a
NBASELIB=$(NBASEDIR)/libnbase.a

CC 	= arm-linux-androideabi-gcc
CFLAGS	= -Wall -O0 -I../include -I$(NBASEDIR)
LDFLAGS	= -L/home/francesco/Dropbox/Informatica/Programmazione/StudioProjects/ANmap/app/src/nmap-src/libpcap  -L/home/francesco/Dropbox/Informatica/Programmazione/StudioProjects/ANmap/app/src/nmap-src/nbase
LIBS	=  /home/francesco/Dropbox/Informatica/Programmazione/StudioProjects/ANmap/app/src/nmap-src/libpcap/libpcap.a

SRC = tests_main.c \
      basic.c \
      timer.c \
      logs.c \
      connect.c \
      ghlists.c \
      ghheaps.c \
      cancel.c

OBJ = $(SRC:.c=.o)

EXE = tests_main

all: $(SRC) $(EXE)

$(EXE): $(OBJ)
	$(CC) $(LDFLAGS) $(OBJ) -o $@ $(NSOCKLIB) $(NBASELIB) $(LIBS)

.c.o:
	$(CC) -c $(CFLAGS) $< -o $@

clean:
	$(RM) $(OBJ) $(EXE)

rebuild: clean $(EXE)

.PHONY: clean rebuild
