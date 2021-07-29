# SPDX-Licence-Identifier: MIT

SUMMARY = "Example recipe for patching and compiling a helloworld C++ application"
LICENSE = "MIT"


# NOTE: This example recipe expands on the "helloworld" recipe, which should
#       be found within the "recipes-examples" directory of the "meta-mion"
#       layer.


# With repect to the previous recipe, the only thing that has changed was adding
# the path of a patch file. Assuming the paths in the patch, and the correct
# source directory is specified in this recipe, this patch will automatically
# be applied.
#
# You may find the following notes useful for wrangling patches.
#
# When patches are applied, by default, bitbake applies a strip level of 1,
# meaning that when the paths of the files to be patched are evaluated, that
# number of leading forward slashes (and their folders) are removed from the
# file search path.
#
# And just like ordinary files, these will be brought into WORKDIR. 

SRC_URI = " \
	file://main.cpp \
	file://LICENSE \
	file://0000-patch-main.patch \
"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c6640a3f1119eaec9540d5a4c69d5aef"


# Just like the previous example, since all the sources are brought into 
# WORKDIR, the source directory needs to be the same.

S = "${WORKDIR}"


CXXFLAGS += " -std=c++17 -O2"


do_compile() {
    ${CXX} ${CXXFLAGS} ${LDFLAGS} ${S}/main.cpp -o ${B}/helloworld_patched
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/helloworld_patched ${D}${bindir}/helloworld_patched
}

FILES_${PN} = "${bindir}"
