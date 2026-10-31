cmake_minimum_required(VERSION 3.22.1)

project(futura_audio LANGUAGES CXX)

add_library(
    futura_audio
    SHARED
    native_bridge.cpp
    AudioEngine.cpp
)

find_package(oboe REQUIRED CONFIG)

target_compile_features(futura_audio PRIVATE cxx_std_17)
target_compile_options(futura_audio PRIVATE -Wall -Wextra -Werror=return-type)
target_link_libraries(futura_audio PRIVATE oboe::oboe log)
