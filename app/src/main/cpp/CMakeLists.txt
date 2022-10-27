# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.18.1)

project("anmapwrapper")

include_directories(src/main/cpp/nmap-7.93/)

add_library(
        nmap-wrapper-lib
        SHARED
        nmap-wrapper-lib.cpp)

add_library(
        nmap-lib
        SHARED
        IMPORTED)

set_target_properties(
        nmap-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libnmap-lib.so)

find_library(
        log-lib
        log )

target_link_libraries(
        nmap-wrapper-lib
        nmap-lib)

target_link_libraries(
        nmap-wrapper-lib
        ${log-lib})