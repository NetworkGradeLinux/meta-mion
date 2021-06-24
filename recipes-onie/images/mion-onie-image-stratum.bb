# SPDX-License-Identifier: MIT

# This image depends on the Barefoot SDE being set up correctly

require mion-onie-image.inc
require recipes-core/images/mion-image-core.inc

ONLPV="onlpv1"
IMAGE_INSTALL_append = " \
    ${ONLPV} \
    packagegroup-onl-python2 \
    p4-compilers \
    stratum \
"
