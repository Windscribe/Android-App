cmake_minimum_required(VERSION 3.22.1)
project("wstunnel")
add_library(${CMAKE_PROJECT_NAME} SHARED
        wstunnel.c)
# Set path to your .so file
set(MY_LIB_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libproxy.so)
message(STATUS "Path to library: ${MY_LIB_PATH}")
target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        ${MY_LIB_PATH})