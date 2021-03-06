inherit image 
SUMMARY = "Create ONIE based mion image"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
INHIBIT_DEFAULT_DEPS = "1"
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
SRC_URI = "file://grub-env/install.sh \
           file://u-boot-env/install.sh \
           file://sharch_body.sh \
	   "

IMAGE_FEATURES[validitems] += "secureboot"

do_bundle[depends] += "mc::${MCNAME}:virtual/kernel:do_deploy"

# Uncomment the following for initramfs only boot
#INSTALL_TYPE = "initramfs"
INSTALL_ROOTFS = "${IMGDEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.tar.xz"
INSTALL_KERNEL = "${DEPLOY_DIR_IMAGE}/bzImage"
INSTALL_INITRD = "${IMGDEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.cpio.gz"

ONIEIMAGE_DIR ?= "${S}"
ONIEIMAGE_DIR_INSTALL = "${ONIEIMAGE_DIR}/installer/"

ONIEIMAGE_SHELL_ARCHIVE_BODY ?= "${WORKDIR}/sharch_body.sh"
ONIEIMAGE_CONF_DIR ?= "${MIONBASE}/meta-mion-bsp/meta-mion-${ONIE_VENDOR}/conf/onie-profiles/${MACHINE}/${ONLPV}"
ONIEIMAGE_MACHINE_CONF ?= "${ONIEIMAGE_CONF_DIR}/machine.conf"
ONIEIMAGE_PLATFORM_CONF ?= "${ONIEIMAGE_CONF_DIR}/platform.conf"

ONIEIMAGE_PAYLOAD_FILE = "${ONIEIMAGE_DIR}/onie_install.tar"
ONIEIMAGE_OUTPUT_FILE = "${ONIEIMAGE_DIR}/onie-installer.bin"

# Uncomment the following to enable Secure Boot
#SECURE_BOOT_ENABLED = "true"
#SECURE_BOOT_SIGNING_KEY = ""
#SECURE_BOOT_SIGNING_CERT = ""
# NOTE: this requires Secure Boot to be enabled in ONIE and the UEFI BIOS
DEPENDS += "${@ "sbsigntool-native" if bb.utils.to_boolean(d.getVar('SECURE_BOOT_ENABLED')) else "" }"

#FIXME remove /boot from cpio.gz
IMAGE_FSTYPES += "tar.xz"
IMAGE_FSTYPES += "${@ "cpio.gz" if bb.utils.to_boolean(d.getVar('SECURE_BOOT_ENABLED')) else "" }"

python () {
        d.delVarFlag("do_fetch", "noexec")
        d.delVarFlag("do_unpack", "noexec")
}

do_bundle () {
    mkdir --parent "${ONIEIMAGE_DIR_INSTALL}"

    # Install script and install file depend on the ARCH
    if [ "${TARGET_ARCH}" = "x86_64" ]; then
        INSTALL_SCRIPT="${WORKDIR}/grub-env/install.sh"
        if [ "${INSTALL_TYPE}" = "initramfs" ]; then
            cp "${INSTALL_KERNEL}" "${ONIEIMAGE_DIR_INSTALL}/"
            cp "${INSTALL_INITRD}" "${ONIEIMAGE_DIR_INSTALL}/mion.initrd"
        else # standard install
            [ ${SECURE_BOOT_ENABLED} = "true" ] && bbfatal "Secure boot requires initramfs install"
            cp "${INSTALL_ROOTFS}" "${ONIEIMAGE_DIR_INSTALL}/rootfs.tar.xz"
        fi
    elif [ "${TARGET_ARCH}" = "arm" ]; then #FIXME!!
        bbfatal "ARM installation not implemented!!"
        #INSTALL_SCRIPT="${WORKDIR}/u-boot-env/install.sh"
        #cp "${INSTALL_FILE}" "${ONIEIMAGE_DIR_INSTALL}/fitImage-mion.bin"
    else
        bbfatal "Unable to parse TARGET_ARCH"
    fi

    if [ ${SECURE_BOOT_ENABLED} = "true" ]; then
        sbsign --key ${SECURE_BOOT_SIGNING_KEY} --cert ${SECURE_BOOT_SIGNING_CERT} ${ONIEIMAGE_DIR_INSTALL}/bzImage
        mv ${ONIEIMAGE_DIR_INSTALL}/bzImage.signed ${ONIEIMAGE_DIR_INSTALL}/bzImage
    fi

    # Generate the payload tar file
    cp "${INSTALL_SCRIPT}" "${ONIEIMAGE_PLATFORM_CONF}" "${ONIEIMAGE_MACHINE_CONF}" "${ONIEIMAGE_DIR_INSTALL}"
    # Set the mion version in the grub menu
    sed -i -e "s/MION_VERSION=/MION_VERSION=\"${DISTRO_VERSION}\"/" "${ONIEIMAGE_DIR_INSTALL}/install.sh"
    sed -i -e "s/ROOTFS_SIZE_MB=/ROOTFS_SIZE_MB=${MION_PART_SIZE_MB}/" "${ONIEIMAGE_DIR_INSTALL}/install.sh"
    sed -i -e "s/INSTALL_TYPE=/INSTALL_TYPE=\"${INSTALL_TYPE}\"/" "${ONIEIMAGE_DIR_INSTALL}/install.sh"
    tar -C "${ONIEIMAGE_DIR}" -cf "${ONIEIMAGE_PAYLOAD_FILE}" "installer"

    # Populate the shell archive body
    cp "${ONIEIMAGE_SHELL_ARCHIVE_BODY}" "${ONIEIMAGE_OUTPUT_FILE}"
    image_sha1=$(sha1sum "${ONIEIMAGE_PAYLOAD_FILE}" | awk '{print $1}')
    sed -i -e "s/%%IMAGE_SHA1%%/${image_sha1}/" "${ONIEIMAGE_OUTPUT_FILE}"
    build_date=$(date +%Y-%m-%d)
    sed -i -e "s/%%BUILD_DATE%%/${build_date}/" "${ONIEIMAGE_OUTPUT_FILE}"

    # Generate the ONIE bin file
    cat "${ONIEIMAGE_PAYLOAD_FILE}" >> "${ONIEIMAGE_OUTPUT_FILE}"
    
    . ${ONIEIMAGE_MACHINE_CONF}
    install -m 0644 ${ONIEIMAGE_OUTPUT_FILE} ${IMGDEPLOYDIR}/onie-installer-${PLATFORM}.bin
    cd ${IMGDEPLOYDIR}
    ln -sf ./onie-installer-${PLATFORM}.bin ./onie-installer.bin
}

IMAGE_POSTPROCESS_COMMAND_append=" do_unpack; do_bundle; "
