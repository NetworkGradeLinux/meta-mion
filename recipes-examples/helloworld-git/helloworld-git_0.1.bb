# SPDX-Licence-Identifier: MIT

SUMMARY = ""
LICENSE = "MIT"

SRC_URI = "git://github.com/NetworkGradeLinux/helloworld;branch=main"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c6640a3f1119eaec9540d5a4c69d5aef"


# Since the source file is being copied into WORKDIR, and patch application 2is
# relative to S (which doesn't exist) we need to set S to equal the WORKDIR.

S = "${WORKDIR}/git"


# When the git fetcher is used, in needs to know what revision of the software
# to use. AUTOREV informs the fetcher to use the latest version.

SRCREV = "${AUTOREV}"


CXXFLAGS += " -std=c++17 -O2"

do_compile() {
    ${CXX} ${CXXLAGS} ${S}/main.cpp ${LDFLAGS} -o ${B}/helloworld_git
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/helloworld_git ${D}${bindir}/helloworld_git
}

FILES_${PN} = "${bindir}"
