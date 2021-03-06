# SPDX-License-Identifier: MIT

SUMMARY = "Create preconfigure script for guest(s)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit allarch

python do_compile () {
    with open(d.getVar('WORKDIR')+"/20-preconfig-guest.conf", 'w+') as conffile:
        for container in (d.getVar('CONTAINER_NAMES') or "").split():
            conffile.write("[guest:{}]\n".format(container))
            conffile.write("image = local:{}\n".format(container))
            if container not in (d.getVar('MION_CONT_DISABLE') or "").split():
                conffile.write("enable = true\n")
            conffile.write("\n")
}

do_install() {
    install -d -m 0755 "${D}${datadir}/srunc/preconfig.d"
    install -m 0644 "${WORKDIR}/20-preconfig-guest.conf" "${D}${datadir}/srunc/preconfig.d"
}

FILES_${PN} = "${datadir}/srunc"
