FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += "file://00-aliases.sh"

do_install_append() {
    install -d ${D}${sysconfdir}/profile.d/
    install -m 0755 ${WORKDIR}/00-aliases.sh ${D}${sysconfdir}/profile.d/
}

#FILES_${PN} = "${sysconfdir}/profile.d"
