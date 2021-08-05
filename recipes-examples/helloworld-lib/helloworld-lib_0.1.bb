# SPDX-Licence-Identifier: MIT

SUMMARY = "Example recipe for patching"
LICENSE = "MIT"


SRC_URI = "file://*"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c6640a3f1119eaec9540d5a4c69d5aef"

S = "${WORKDIR}"

inherit cmake


