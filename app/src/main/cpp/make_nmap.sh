#!/usr/bin/env bash

NMAP_VERSION='7.94'
NMAP_SRC="nmap-${NMAP_VERSION}.tgz"
NMAP_DOWNLOAD_URL="https://nmap.org/dist/${NMAP_SRC}"
NMAP_BUILD_DIR="nmap-${NMAP_VERSION}"
NDK="$(ls -dr /home/${USER}/Android/Sdk/ndk/* | head -1)"
HOST_ARCH='linux-x86_64'

NMAP_ASSETS=('nmap-service-probes'
             'nmap-services'
             'nmap-protocols'
             'nmap-rpc'
             'nmap-mac-prefixes'
             'nmap-os-db')

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
  rm -rf libs
  for target in "${!ANDROID_TARGETS_ABI[@]}"; do
    mkdir -p "libs/${ANDROID_TARGETS_ABI[$target]}"
  done
}

# Extracts Nmap source. Removes it before, if it already exists.
function prepare_source() {
  if ! [[ -f "${NMAP_SRC}" ]]; then
    wget "${NMAP_DOWNLOAD_URL}" -O "${NMAP_SRC}"
  fi
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
  make STATIC='-static-libstdc++'
  cp nmap "../libs/${ANDROID_TARGETS_ABI[$TARGET]}/libnmap.so"
}

function import_nmap_assets_in_android_project() {
  for asset in "${NMAP_ASSETS[@]}"; do
    cp "${NMAP_BUILD_DIR}/${asset}" ../assets/"${asset}"
  done
}

function main() {
  create_lib_folders
  for target in "${!ANDROID_TARGETS_ABI[@]}"; do
    prepare_source
    cd "${NMAP_BUILD_DIR}"
    cross_compile_nmap "${target}"
    cd ..
  done
  import_nmap_assets_in_android_project
}

main "$@"
