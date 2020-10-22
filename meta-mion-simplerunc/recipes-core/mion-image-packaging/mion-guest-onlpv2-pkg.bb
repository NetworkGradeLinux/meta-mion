# SPDX-License-Identifier: MIT

SUMMARY = "Package onlpv2 container image"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
FILESEXTRAPATHS_prepend = "${TOPDIR}/tmp-guest-${MACHINE}/deploy/images/${MACHINE}:"

INHIBIT_DEFAULT_DEPS = "1"

inherit allarch
do_fetch[nostamp]="1"
do_install[nostamp]="1"

do_fetch[mcdepends] = "mc:${HOSTMC}:${GUESTMC}:mion-guest-onlpv2:do_image_complete mc:${HOSTMC}:${GUESTMC}:virtual/kernel:do_deploy"

python () {
    import os
    tmp=d.getVar('BUILD_ARGS', True).split(' ')
    if not tmp:
        raise bb.parse.SkipRecipe("Recipe called outside of mc_build. See release note in RELEASE_NOTES on multitarget mc builds.")

    for i, s in enumerate(tmp):
        if "host" in s:
            d.setVar("HOSTMC", s.split(":")[0])
        if "mion-guest-onlpv2" in s:
            d.setVar("GUESTMC", s.split(":")[0])
}

do_install () {
 install -d ${D}/usr/share/srunc/local-feed/guest/mion-guest-onlpv2

 install ${TOPDIR}/tmp-guest-${MACHINE}/deploy/images/${MACHINE}/mion-guest-onlpv2-${MACHINE}.tar.xz ${D}/usr/share/srunc/local-feed/guest/mion-guest-onlpv2/
}

FILES_${PN} += "${prefix}/share/srunc/local-feed/*"

RDEPENDS_${PN} += "systemd-container" 
