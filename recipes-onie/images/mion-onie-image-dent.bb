# SPDX-License-Identifier: MIT
require mion-onie-image.inc
require recipes-core/images/mion-image-core.inc
ONLPV="dent"
IMAGE_INSTALL_append = " \
    dent \
    ifupdown2 \
    packagegroup-onl-python2 \
"
