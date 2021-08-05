# SPDX-Licence-Identifier: MIT

SUMMARY = "Example recipe for compiling a helloworld C++ application"
LICENSE = "MIT"


SRC_URI = " \
	file://main.cpp \
	file://LICENSE \
"


LIC_FILES_CHKSUM = "file://LICENSE;md5=c6640a3f1119eaec9540d5a4c69d5aef"


S = "${WORKDIR}"


# RDEPENDS defines build time dependencies, and DEPENDS defines build-time
# dependencies. The runtime shared object is provided b

RDEPENDS_${PN} = 'helloworld-lib'
DEPENDS = "helloworld-lib"

CXXFLAGS += " -std=c++17 -O2" 
LDFLAGS += " -lhelloworld_shared"

do_compile() {
    ${CXX} ${CXXFLAGS} ${WORKDIR}/main.cpp ${LDFLAGS} -o ${B}/helloworld_shareddep
}


do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/helloworld ${D}${bindir}/helloworld
}


FILES_${PN} = "${bindir}"
