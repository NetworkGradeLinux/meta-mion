# SPDX-License-Identifier: MIT
require mion-onie-image.inc
require recipes-core/images/mion-image-core.inc

ONLPV="onlpv1"
IMAGE_INSTALL += " \
    ${ONLPV} \
    packagegroup-onl-python2 \
"
