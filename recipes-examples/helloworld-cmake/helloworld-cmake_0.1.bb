# SPDX-Licence-Identifier: MIT

SUMMARY = "Example recipe for patching and compiling a helloworld C++ application"
LICENSE = "MIT"


SRC_URI = " \
	file://main.cpp \
	file://CMakeLists.txt \
	file://LICENSE \
	file://0000-patch-main.patch \
"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c6640a3f1119eaec9540d5a4c69d5aef"

S = "${WORKDIR}"

inherit cmake

