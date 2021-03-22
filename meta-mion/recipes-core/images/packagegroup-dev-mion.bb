SUMMARY = "Mion development packages"
DESCRIPTION = "Packages required for development on a system"
PR = "r1"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

RDEPENDS_${PN} = "\
    ccache \
    dmidecode \
    git \
    kernel-devsrc \
    m4 \
    meson \
    ninja \
    packagegroup-core-buildessential \
    packagegroup-core-tools-debug \
"

#RRECOMMENDS_${PN} = ""
