# SPDX-License-Identifier: MIT

DESCRIPTION = "Simple Runc Application"
HOMEPAGE = "https://mion.io/"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit allarch systemd

PV = "0.4.0"

SRC_URI = "file://srunc.py \
           file://srunc-test.py \
           file://srunc-guests.service \
    "

do_install() {
    install -d "${D}${base_sbindir}"
    install -T -m 0755 ${WORKDIR}/srunc.py "${D}${base_sbindir}/srunc"
    install -T -m 0755 ${WORKDIR}/srunc-test.py "${D}${base_sbindir}/srunc-test"
    install -d "${D}${systemd_unitdir}/system/"
    install -m 0644 ${WORKDIR}/srunc-guests.service "${D}${systemd_unitdir}/system/"
}

#PACKAGES = "srunc srunc-test "

RDEPENDS_srunc = " \
    python3-compression \
    python3-fcntl \
    python3-json \
    python3-logging \
    python3-netclient \
    python3-shell \
    "

RDEPENDS_srunc-test = " \
    srunc \
    python3-unittest \
    python3-betatest \
    "

SYSTEMD_PACKAGES = "srunc"
SYSTEMD_SERVICE_srunc = "srunc-guests.service"
