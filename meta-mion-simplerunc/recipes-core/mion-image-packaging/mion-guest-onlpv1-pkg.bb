# SPDX-License-Identifier: MIT

SUMMARY = "Package onlpv1 container image"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
DEPENDS = "mion-guest-onlpv1"
FILESEXTRAPATHS_prepend = "${DEPLOY_DIR}/images/${MACHINE}:"
SRC_URI = "file://mion-guest-onlpv1-${MACHINE}.tar.xz"
SRC_URI[md5sums] = ""
do_fetch[deptask] = "do_image_complete"
do_compile[noexec] = "1"
do_install () {
 install -d ${D}/var/lib/machines
 install ${DEPLOY_DIR}/images/${MACHINE}/mion-guest-onlpv1-${MACHINE}.tar.xz ${D}/var/lib/machines
}
RDEPENDS_${PN} += "systemd-container" 
