# SPDX-Licence-Identifier: MIT

SUMMARY = ""
LICENSE = "MIT"


# In this example, all of the sources are within a TAR file. BitBake will
# automatically extract the archive. The contents of this archive do not have
# any leading paths; this will be important with repsect to the source
# directory.
#
# Another common pattern you will notice here is adding patches on top of an
# unpacked archive -- it wouldn't be typical for a source archive to contain
# patches. The utility of this is that we can alter an otherwise complete code
# base for things like security, hardening, and making it easier to build with
# BitBake, among others. 

SRC_URI = " \
	file://helloworld-tar-1.0.0.tgz \
    file://0000-patch-main.patch \
"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c6640a3f1119eaec9540d5a4c69d5aef"


# As alluded to in the SRC_URI comment, you may need to consider what the value
# of the source directory should be. In this case, since there are no directory
# prefixes within the archive, once again, the source directory is the same as
# the working directory. However, it is much more common to have archives with
# files that have a leading path of the archive name. The consideration here is
# that this prefix must be appended to the WORKDIR to form S. For example, out
# archive could have the files (relative to the archive root):
#
#    /helloworld-tar
#
# Or with a version specified:
#
#    /helloworld-tar-1.0.0
#
# In any of the cases, this directory must be added. e.g
#
#    S = "${WORKDIR}/helloworld-tar-1.0.0"

S = "${WORKDIR}"


#CPPFLAGS += " -std=c++17 -O2"


do_compile() {
    ${CXX} ${CPPFLAGS} ${LDFLAGS} ${S}/main.cpp -o ${B}/helloworld_tar
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/helloworld_tar ${D}${bindir}/helloworld_tar
}

FILES_${PN} = "${bindir}"
