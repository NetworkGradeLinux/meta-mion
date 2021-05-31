require mion-onie-image.inc
require recipes-core/images/mion-image-core.inc
ONLPV="dent"
IMAGE_INSTALL += " \
 ${ONLPV} \
 packagegroup-onl-python2 \
"

IMAGE_FSTYPES += "${INITRAMFS_FSTYPES}"

PACKAGE_INSTALL += "${IMAGE_INSTALL}"

INSTALL_FILE="${IMGDEPLOYDIR}/fitImage-${IMAGE_BASENAME}-${MACHINE}-${MACHINE}"

