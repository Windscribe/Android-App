cmake_minimum_required(VERSION 3.22.1)

project(wscommon C)

add_library(wscommon SHARED packet_mux.c)

# Android 15+ devices with 16 KB pages reject 4 KB-aligned segments.
target_link_options(wscommon PRIVATE "-Wl,-z,max-page-size=16384")
