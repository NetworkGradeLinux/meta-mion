DESCRIPTION = "Start script for sshd"
HOMEPAGE = "https://www.mion.io"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit allarch

PV = "1.0.0"

SRC_URI = "file://start-sshd"

do_install() {
    install -m 0755 -d ${D}${base_sbindir}
    install -m 0755 ${WORKDIR}/start-sshd ${D}${base_sbindir}
}

#FILES_${PN} = " \
#    ${base_sbindir}/start-sshd \
#    "
