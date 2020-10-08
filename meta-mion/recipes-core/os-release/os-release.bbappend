# SPDX-License-Identifier: MIT

PACKAGE_ARCH = "${MACHINE_ARCH}"

OS_RELEASE_FIELDS_append_mion = " \
    BUILD_ID \
    HOME_URL \
    MION_MACHINE \
    "

HOME_URL = "http://mion.io/"
BUILD_ID = "${MION_BUILD_ID}"
MION_MACHINE = "${MACHINE}"
