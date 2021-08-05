# SPDX-Licence-Identifier: MIT

SUMMARY = "Example recipe for compiling a helloworld C++ application"
LICENSE = "MIT"



# The SRC_URI is a tool with many talents. It's purpose is to bring in files
# from different sources into the build tree for the recipe. It can bring in
# ordinary files, pull git repositories, unpack tar achives, and apply patches.
#
# In this case, we are only interested in files.
#
# The paths of files specified here are, by default, relative to a directory
# named with the name of the recipe, and within the recipe directory.
#
# In this case, the directory search here subdirectory called "helloworld".
#
# It is possible to specify a different directory, which is a common pattern
# within mion recipes. This is done as follows:
#
#    FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
#

SRC_URI = " \
	file://main.cpp \
	file://LICENSE \
"


# The licence checksum is required in mion. It's goal is to make sure that any
# software pulled hasn't had any license changes.

LIC_FILES_CHKSUM = "file://LICENSE;md5=c6640a3f1119eaec9540d5a4c69d5aef"


# The "S" variable represents the source directory, and is the relative path by
# which many operations are performed. The "WORKDIR" represents the top of the
# recipe's build tree. In many cases, these are different. Tar archives may have
# their packed files with some directory prefix, which would be expanded upon
# unpacking into the WORKDIR. Git repositories are pulled into a "git" directory
# within WORKDIR.
#
# In this case, because we are copying plain files, which are placed directly
# into the WORKDIR, the source directory S needs to be the same.

S = "${WORKDIR}"


# The build system will already have a set of flags defined for things like
# hardening, hashing etcetera. Because of this, we don't want to clobber what
# has already been set, and instead append to it. If you discover that there is
# some flag provided by default that is not desirable, it can be removed via
#
#    CXXFLAGS_remove = " -someflag"
#
# However, this is probably not something you should be doing.

CXXFLAGS += " -std=c++17 -O2"


# `do_compile` is the function responsible for compileing a recipe.
#
# Just like CPPFLAGS, LDFLAGS is already helpfully set by the build system.
#
# Here we also have a new variable, B. This is the build directory, and is where
# all artefacts produced by recipe should be placed. As is common practice these
# days, source and build output directories are separated.
#
# While there is a variable called "CPP" do not make the mistake of using it
# in place of "CXX"! "CPP" is the minimal command and arguments to run the 
# C pre-processor!

do_compile() {
    ${CXX} ${CXXFLAGS} ${WORKDIR}/main.cpp ${LDFLAGS} -o ${B}/helloworld
}


# `do_install` is the function responsible for installing build artefacts into
# the destination directory.
#
# Funnily enough, "D" stands for the destination path, in which all artefacts
# are staged prior to building a package.
#
# There is also the "bindir" variable. This is the standard filesystem path for
# the location of executables on a Linux system. This is typically "/usr/bin",
# but it can be overriden.
#
# What is basically happening is that all of the artefacts are being staged in
# directory structure representative of the target system.
#
# You might expext that "bindir" should already exist, however, because all
# packages stage their results in their own directory, it does still need to
# be created. However, any other recipes dependent on the package this recipe
# produces will be able to see them.

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/helloworld ${D}${bindir}/helloworld
}


# Finally, FILES indicates which directories and files should be included in
# the finished package. In mion, if the build directory contains a file which
# isn't listed here, an error will be produced.

FILES_${PN} = "${bindir}"
