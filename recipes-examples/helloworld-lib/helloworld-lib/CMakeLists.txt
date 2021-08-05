project(HelloWorldLib-CMake)
cmake_minimum_required(VERSION 3.12)


option(BUILD_SHARED_LIBS "Build using shared libraries" ON)

add_subdirectory(src)
