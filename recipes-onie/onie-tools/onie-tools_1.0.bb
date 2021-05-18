# SPDX-License-Identifier: MIT

DESCRIPTION = "Linux ONIE tools"
LICENSE = "MIT"
SECTION = "base"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

#FIXME add support for ARM tools
#RDEPENDS_onie-tools = "bash ${@oe.utils.conditional('ARCH', 'u-boot-arch', ' u-boot-fw-utils', '', d)}"

SRC_URI = " \
  file://common-blkdev \
  file://onie-install \
  file://onie-uninstall \
  file://onie-update \
  file://onie-rescue \
"

S = "${WORKDIR}"

do_install () {
  install -m 0755 -d ${D}${libdir}/onie/
  install -m 0755 common-blkdev ${D}${libdir}/onie/
  install -m 0755 -d ${D}${bindir}
  install -m 0755 onie-install ${D}${bindir}
  install -m 0755 onie-uninstall ${D}${bindir}
  install -m 0755 onie-update ${D}${bindir}
  install -m 0755 onie-rescue ${D}${bindir}
}

FILES_${PN} = "/"
