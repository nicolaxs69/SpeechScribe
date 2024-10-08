
#
# This file is based on code from the public repository:
# Repository: [android-opus-codec](https://github.com/theeasiestway/android-opus-codec)
# Author: Loboda Alexey
# Original Creation Date: 21.05.2020
# Modified by: Nicolas Escobar on 13/09/2024
# Changes: Updated the CMakeLists.txt file to be compatible with the latest 3.22.1 version of the Android NDK.
#

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        easyopus

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        codec/CodecOpus.cpp
        utils/SamplesConverter.cpp
        easyopus.cpp)

include_directories(${PROJECT_SOURCE_DIR}/include)

#add other prebuilt libraries
add_library(opus SHARED IMPORTED)
add_library(opusenc SHARED IMPORTED)

set_target_properties(opus PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/lib/${ANDROID_ABI}/libopus.so)
set_target_properties(opusenc PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/lib/${ANDROID_ABI}/libopusenc.so)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        easyopus
        opus
        opusenc
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})