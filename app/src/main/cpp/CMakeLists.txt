cmake_minimum_required(VERSION 3.22.1)

project("llama-android")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(
    log-lib
    log
)

add_library(
    llama-android
    SHARED
    llama-android-jni.cpp
)

target_link_libraries(
    llama-android
    ${CMAKE_CURRENT_SOURCE_DIR}/lib/${ANDROID_ABI}/libllama.a
    ${CMAKE_CURRENT_SOURCE_DIR}/lib/${ANDROID_ABI}/libggml.a
    ${CMAKE_CURRENT_SOURCE_DIR}/lib/${ANDROID_ABI}/libggml-cpu.a
    ${CMAKE_CURRENT_SOURCE_DIR}/lib/${ANDROID_ABI}/libggml-base.a
    ${log-lib}
    -fopenmp
    android
)
