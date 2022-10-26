#!/usr/bin/env bash

NMAP_VERSION='7.93'
NMAP_SRC="nmap-${NMAP_VERSION}.tgz"
NMAP_BUILD_DIR="nmap-${NMAP_VERSION}"
NDK="/home/${USER}/Android/Sdk/ndk/25.1.8937393"
HOST_ARCH='linux-x86_64'

declare -A ANDROID_TARGETS_ABI=(['aarch64-linux-android']='arm64-v8a' \
                                ['armv7a-linux-androideabi']='armeabi-v7a')
                                #['i686-linux-android']='x86' \
                                #['x86_64-linux-android']='x86_64')

# Exports variables needed to cross-compile for Android.
# Args:
#   $1 Target (from ANDROID_TARGETS)
function export_make_toolchain() {
  export TARGET="$1"
  export TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/${HOST_ARCH}"
  export API=31
  export AR="${TOOLCHAIN}/bin/llvm-ar"
  export CC="${TOOLCHAIN}/bin/${TARGET}${API}-clang"
  export AS="${CC}"
  export CXX="${TOOLCHAIN}/bin/${TARGET}${API}-clang++"
  export LD="${TOOLCHAIN}/bin/ld"
  export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
  export STRIP="${TOOLCHAIN}/bin/llvm-strip"
}

# Initializes the folder structure for libraries.
function create_lib_folders() {
  mkdir -p 'libs'
  for target in "${!ANDROID_TARGETS_ABI[@]}"; do
    mkdir -p "libs/${ANDROID_TARGETS_ABI[$target]}"
  done
}

# Extracts Nmap source. Removes it before, if it already exists.
function prepare_source() {
  rm -rf "${NMAP_BUILD_DIR}"
  tar -xzf "${NMAP_SRC}"
}

# Cross-compiles nmap for a specified android target.
# Args:
#   $1 Target (from ANDROID_TARGETS)
function cross_compile_nmap() {
  export_make_toolchain "$1"
  ./configure --host "${TARGET}" \
              --without-ncat \
              --without-liblua \
              --without-nping \
              --without-zenmap \
              --without-ndiff \
              --with-libpcap=included \
              --with-liblinear=included \
              --with-libssh2=included \
              --with-libpcre=included \
              --with-libz=included
  make CXXFLAGS='-g -O2 -Wall -fno-strict-aliasing -fPIC' CCOPT='-fPIC'
  "${CXX}" -Wl,-E \
           -Wl,-soname,libnmap-lib.so \
           -static-libstdc++ \
           -Lnbase \
           -Lnsock/src/ \
           -fPIC \
           --shared \
           -o "../libs/${ANDROID_TARGETS_ABI[$TARGET]}/libnmap-lib.so" \
           charpool.o \
           FingerPrintResults.o \
           FPEngine.o \
           FPModel.o \
           idle_scan.o \
           MACLookup.o \
           nmap_dns.o \
           nmap_error.o \
           nmap.o \
           nmap_ftp.o \
           NmapOps.o \
           NmapOutputTable.o \
           nmap_tty.o \
           osscan2.o \
           osscan.o \
           output.o \
           payload.o \
           portlist.o \
           portreasons.o \
           protocols.o \
           scan_engine.o \
           scan_engine_connect.o \
           scan_engine_raw.o \
           scan_lists.o \
           service_scan.o \
           services.o string_pool.o \
           NewTargets.o \
           TargetGroup.o \
           Target.o \
           targets.o \
           tcpip.o \
           timing.o \
           traceroute.o \
           utils.o \
           xml.o  \
           -lnsock \
           -lnbase \
           libpcre/libpcre.a \
           libpcap/libpcap.a   \
           libz/libz.a \
           libnetutil/libnetutil.a \
           ./libdnet-stripped/src/.libs/libdnet.a  \
           ./liblinear/liblinear.a
}

function main() {
  create_lib_folders
  for target in "${!ANDROID_TARGETS_ABI[@]}"; do
    prepare_source
    cd "${NMAP_BUILD_DIR}"
    cross_compile_nmap "${target}"
    cd ..
  done
}

main "$@"
