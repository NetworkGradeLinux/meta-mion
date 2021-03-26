# SPDX-License-Identifier: MIT

PACKAGE_ARCH = "${MACHINE_ARCH}"

OS_RELEASE_FIELDS_append_mion = " \
    BUILD_ID \
    HOME_URL \
    MION_MACHINE \
    "

HOME_URL = "http://mion.io/"
BUILD_ID = "${DISTRO_VERSION} ${DATETIME}"
BUILD_ID[vardepsexclude] = "DATETIME"
MION_MACHINE = "${MACHINE}"
