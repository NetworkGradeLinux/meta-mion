add_library(helloworld_shared SHARED helloworldlib.cpp)
add_library(helloworld_static STATIC helloworldlib.cpp)


# bitbake expects to copy versioned so files for a target install. Typcially
# unversioned so files are used for development purposes.

set_target_properties(helloworld_shared PROPERTIES VERSION 1.0.0 SOVERSION 1)


# We don't have any private header files, so we don't need to specify anything
# like that here.

target_include_directories(helloworld_shared
	PUBLIC
		$<INSTALL_INTERFACE:include>
        $<BUILD_INTERFACE:${PROJECT_SOURCE_DIR}/include>
)

target_include_directories(helloworld_static
	PUBLIC
		$<INSTALL_INTERFACE:include>
        $<BUILD_INTERFACE:${PROJECT_SOURCE_DIR}/include>
)


install(TARGETS helloworld_shared LIBRARY DESTINATION lib)
install(TARGETS helloworld_static
	ARCHIVE DESTINATION lib)
#	INCLUDES DESTINATION include)

install(DIRECTORY ${PROJECT_SOURCE_DIR}/include/apsn DESTINATION include)


