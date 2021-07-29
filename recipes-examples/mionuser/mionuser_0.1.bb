SUMMARY = "Add mion user"
DESCRIPTION = ""

LICENSE = "MIT"


FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += " \
    git://github.com/jacoborus/tender.vim;branch=master;name=tender;destsuffix=tender \
    file://tmux.conf \
    file://mionsudo \
    file://mionenv.conf \
    file://vimrc \
"

SRCREV_tender = "${AUTOREV}"

LIC_FILES_CHKSUM = "\
    file://tender/LICENSE;md5=bd6d4fdab0a2bd1f6a50afa85e84d22c \
"


S = "${WORKDIR}"


DEPENDS += " bash"


inherit useradd


USERADD_PACKAGES = "${PN}"
USERADD_PARAM_${PN} = " -u 1001 -d /home/mion -p SNmjdnWRLfayA -r -s /bin/bash mion"

HOMEDIR = "/home/mion/"

do_install() {
    install -d ${D}${HOMEDIR}
    install -m 0600 ${S}/tmux.conf ${D}${HOMEDIR}.tmux.conf

    # Sudoers conf files must not have a dot in name. So no extensions!
    install -m 0644 -d ${D}${sysconfdir}/sudoers.d
    install -m 0644 ${S}/mionsudo ${D}${sysconfdir}/sudoers.d/

    install -d ${D}${sysconfdir}/profile.d
    #install -m 0644 ${S}/mionenv.conf ${D}${sysconfdir}/profile.d/
    cat ${S}/mionenv.conf >> ${D}${HOMEDIR}/.profile

    install -d ${D}${HOMEDIR}.vim/colors
    install -m 0600 ${S}/vimrc ${D}${HOMEDIR}/.vimrc
    install -m 0600 ${S}/tender/colors/tender.vim ${D}${HOMEDIR}.vim/colors/

    chown -R mion:mion ${D}${HOMEDIR}
}

FILES_${PN} = " \
     ${HOMEDIR} \
     ${sysconfdir} \
"
