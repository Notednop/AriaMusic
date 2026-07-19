cmake_minimum_required(VERSION 3.22.1)

project("usb_audio_engine")

add_library(
    usb_audio_engine
    SHARED
    usb_audio_engine.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    usb_audio_engine
    ${log-lib}
    android
)
