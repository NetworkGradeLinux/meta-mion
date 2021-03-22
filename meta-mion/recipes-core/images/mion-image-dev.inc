SUMMARY = "mion dev packages"
LICENSE = "MIT"

IMAGE_INSTALL_append = " \
    ccache \
    dmidecode \
    git \
    kernel-devsrc \
    m4 \
    meson \
    ninja \
    packagegroup-core-buildessential \
    packagegroup-core-tools-debug \
    pkgconf \
"
