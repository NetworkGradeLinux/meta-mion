# Functions used to generate an ONIE installer file
#
# Copyright (C) 2020 APS Networks
# TODO license

# onieimage_create must be used in an image recipe
# IMAGE_POSTPROCESS_COMMAND += " onieimage_create"

ONIEIMAGE_DIR ?= "${TMPDIR}/onieimage"
ONIEIMAGE_DIR_INSTALL = "${ONIEIMAGE_DIR}/install/"

ONIEIMAGE_SHELL_ARCHIVE_BODY ?= "${TOPDIR}/meta-mion/files/onie/sharch_body.sh"
ONIEIMAGE_INSTALL_SCRIPT ?= "${TOPDIR}/meta-mion/files/onie/grub-env/install.sh"
ONIEIMAGE_CONF_DIR ?= "${TOPDIR}/meta-mion-${ONIE-VENDOR}/recipes-core/platform-${ONIE_MACHINE_TYPE}-${ONLP-VERSION}"
ONIEIMAGE_MACHINE_CONF ?= "${ONIEIMAGE_CONF_DIR}/machine.conf"
ONIEIMAGE_PLATFORM_CONF ?= "${ONIEIMAGE_CONF_DIR}/platform.conf"

ONIEIMAGE_PAYLOAD_FILE = "${ONIEIMAGE_DIR}/onie_install.tar"
ONIEIMAGE_OUTPUT_FILE = "${ONIEIMAGE_DIR}/${IMAGE_NAME}-onie.bin"

onieimage_create() {
    #FIXME how does this get cleaned?
    mkdir --parent "${ONIEIMAGE_DIR_INSTALL}"

    # Generate the payload tar file
    cp "${ONIEIMAGE_INSTALL_SCRIPT}" "${ONIEIMAGE_PLATFORM_CONF}" "${ONIEIMAGE_MACHINE_CONF}" "${ONIEIMAGE_DIR_INSTALL}"
    #FIXME for arm machines the bzImage should be a fitImage containing DTB etc.
    cp "${DEPLOY_DIR_IMAGE}/bzImage" "${ONIEIMAGE_DIR_INSTALL}/bzImage"
    cp "${DEPLOY_DIR_IMAGE}/${IMAGE_NAME}.rootfs.cpio.xz" "${ONIEIMAGE_DIR_INSTALL}/initramfs"
    tar -C "${ONIEIMAGE_DIR}" -cf "${ONIEIMAGE_PAYLOAD_FILE}" "install"

    # Populate the shell archive body
    cp "${ONIEIMAGE_SHELL_ARCHIVE_BODY}" "${ONIEIMAGE_OUTPUT_FILE}"
    image_sha1=$(sha1sum "${ONIEIMAGE_PAYLOAD_FILE}" | awk '{print $1}')
    sed -i -e "s/%%image_sha1%%/${image_sha1}/" "${ONIEIMAGE_OUTPUT_FILE}"
    #TODO add the build date to the shell archive
    #build_date=$(date +%Y-%m-%d)
    #sed -i -e "s/%%build_date%%/${build_date}/" "${ONIEIMAGE_OUTPUT_FILE}"

    # Generate the ONIE bin file and copy to the deploy image dir
    cat "${ONIEIMAGE_PAYLOAD_FILE}" >> "${ONIEIMAGE_OUTPUT_FILE}"
    cp "${ONIEIMAGE_OUTPUT_FILE}" "${DEPLOY_DIR_IMAGE}"
}
