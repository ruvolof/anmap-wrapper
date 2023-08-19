# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.18.1)

project("anmapwrapper")

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/nmap-7.94/)

add_executable(
        libnmap-wrapper.so
        nmap-wrapper.cpp)

add_library(
        nmap-lib
        SHARED
        IMPORTED)

set_target_properties(
        nmap-lib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libnmap-lib.so)

target_link_libraries(
        libnmap-wrapper.so
        nmap-lib)