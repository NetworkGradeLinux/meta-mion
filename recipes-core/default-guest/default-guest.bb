SUMMARY = "Add the default guest and set it to autostart"
LICENSE = "CLOSED"

inherit allarch

SRC_URI = "file://20-default-guest.conf"

do_install() {
    install -d -m 0755 "${D}${datadir}/oryx/preconfig.d"
    install -m 0644 "${WORKDIR}/20-default-guest.conf" "${D}${datadir}/oryx/preconfig.d"
}

FILES_${PN} = "${datadir}/oryx"
