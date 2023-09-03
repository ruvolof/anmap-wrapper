# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.18.1)

project("anmapwrapper")

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/nmap-7.94/)

add_executable(
        do_nothing.so
        do_nothing.cpp)

add_library(
        nmap
        SHARED
        IMPORTED)

set_target_properties(
        nmap
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libnmap.so)

target_link_libraries(
        do_nothing.so
        nmap)