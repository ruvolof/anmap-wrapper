#!/usr/bin/env bash
PREFIXBIN="arm-linux-androideabi"

PATH=/home/francesco/Dropbox/bin/android-toolchain/bin:$PATH
CC=$PREFIXBIN-gcc
CXX=$PREFIXBIN-g++
LD=$PREFIXBIN-ld
RANLIB=$PREFIXBIN-ranlib
AR=$PREFIXBIN-ar
STRIP=$PREFIXBIN-strip
CFLAGS="-fvisibility=default -fPIE"
CXXFLAGS="-fvisibility=default -fPIE"
LDFLAGS="-rdynamic -pie"

./configure \
    --host=arm-linux-androideabi \
    --without-zenmap \
    --with-liblua=included \
    --with-pcap=linux \
    --with-libpcap=internal \
    --enable-static \
    --without-ndiff \
    --without-nping \
    --without-nmap-update \
    --without-ncat

make

echo "Compiling nmap-wrapper.o."
arm-linux-androideabi-g++ -c -I./liblinear -I./liblua -I./libdnet-stripped/include -I./libpcre  -I./libpcap -I./nbase -I./nsock/include -DHAVE_CONFIG_H -DNMAP_NAME=\"Nmap\" -DNMAP_URL=\"https://nmap.org\" -DNMAP_PLATFORM=\"arm-unknown-linux-androideabi\" -DNMAPDATADIR=\"/usr/local/share/nmap\" -D_FORTIFY_SOURCE=2 -g -O2 -Wall -fno-strict-aliasing nmap-wrapper.cc -o nmap-wrapper.o

echo "Compiling libnmap-wrapper.so."
arm-linux-androideabi-g++ -Wl,-E  -Lnbase -Lnsock/src/ --shared -o libnmap-wrapper.so charpool.o FingerPrintResults.o FPEngine.o FPModel.o idle_scan.o MACLookup.o nmap_dns.o nmap_error.o nmap.o nmap_ftp.o NmapOps.o NmapOutputTable.o nmap_tty.o osscan2.o osscan.o output.o payload.o portlist.o portreasons.o protocols.o scan_engine.o scan_engine_connect.o scan_engine_raw.o service_scan.o services.o TargetGroup.o Target.o targets.o tcpip.o timing.o traceroute.o utils.o xml.o nse_main.o nse_utility.o nse_nsock.o nse_dnet.o nse_fs.o nse_nmaplib.o nse_debug.o nse_pcrelib.o nse_binlib.o nse_bit.o nse_lpeg.o nmap-wrapper.o -lnsock -lnbase libpcre/libpcre.a libpcap/libpcap.a  libnetutil/libnetutil.a ./libdnet-stripped/src/.libs/libdnet.a ./liblua/liblua.a ./liblinear/liblinear.a

echo "Moving libnmap-wrapper.so into PROJECT_HOME/app/src/main/jniLibs/armeabi."
mv libnmap-wrapper.so ../main/jniLibs/armeabi/libnmap-wrapper.so