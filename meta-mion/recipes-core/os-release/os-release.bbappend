# SPDX-License-Identifier: MIT

OS_RELEASE_FIELDS_append_mion = " \
    BUILD_ID \
    HOME_URL \
    "

HOME_URL = "http://mion.io/"
BUILD_ID = "${DISTRO_VERSION} ${DATETIME}"
BUILD_ID[vardepsexclude] = "DATETIME"
