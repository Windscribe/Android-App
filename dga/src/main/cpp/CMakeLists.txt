cmake_minimum_required(VERSION 3.10)

project(dga LANGUAGES CXX)

set(CMAKE_INCLUDE_CURRENT_DIR ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (${MSVC})
    #MSVS static linking
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
else ()
    #set(CMAKE_CXX_VISIBILITY_PRESET hidden)
    #set(CMAKE_VISIBILITY_INLINES_HIDDEN True)
endif ()

#add_definitions(-D_CRT_SECURE_NO_WARNINGS)

#set(BOOST_PATH C:/Work/client-desktop-public/build-libs/boost/include)
#set(BOOST_PATH /Users/aaa/Documents/client-desktop-public/build-libs/boost/include)
#set(BOOST_PATH /home/aaa/Documents/client-desktop-public/build-libs/boost/include)

#set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MT")

add_library(dga-library SHARED
        native-lib.cpp
        )
#add_subdirectory(domain_generator)
#add_subdirectory(library)
#add_subdirectory(show_domain)
#add_subdirectory(tests)
#add_subdirectory(strip_vc_util)

#install(TARGETS domain_generator RUNTIME DESTINATION bin)
#install(TARGETS dga RUNTIME DESTINATION bin)
#install(TARGETS show_domain RUNTIME DESTINATION bin)
#install(TARGETS strip_vc_util RUNTIME DESTINATION bin)
