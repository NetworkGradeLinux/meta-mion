# SPDX-License-Identifier: MIT
require mion-onie-image.inc
require recipes-core/images/mion-image-core.inc
ONLPV="onlpv2"
IMAGE_INSTALL_append = " \
    ${ONLPV} \
    packagegroup-onl-python2 \
"
