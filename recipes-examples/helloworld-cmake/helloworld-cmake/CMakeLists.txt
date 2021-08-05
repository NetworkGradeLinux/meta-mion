project(HelloWorld-CMake)
cmake_minimum_required(VERSION 3.12)

add_executable(helloworld_cmake main.cpp)

install(TARGETS helloworld_cmake RUNTIME DESTINATION bin)
