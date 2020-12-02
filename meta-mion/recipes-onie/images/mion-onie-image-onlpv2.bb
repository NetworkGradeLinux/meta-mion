# SPDX-License-Identifier: MIT
require mion-onie-image.inc
require recipes-core/images/mion-image-core.inc
ONLPV="onlpv2"
IMAGE_INSTALL += " \
 ${ONLPV} \
 packagegroup-onl-python2 \
"

INSTALL_FILE="${IMGDEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.tar.xz"

