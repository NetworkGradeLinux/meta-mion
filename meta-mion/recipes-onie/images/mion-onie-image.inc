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
ONIEIMAGE_DIR ?= "${S}"
ONIEIMAGE_DIR_INSTALL = "${ONIEIMAGE_DIR}/installer/"

ONIEIMAGE_SHELL_ARCHIVE_BODY ?= "${WORKDIR}/sharch_body.sh"
ONIEIMAGE_CONF_DIR ?= "${MIONBASE}/meta-mion-bsp/meta-mion-${ONIE_VENDOR}/conf/onie-profiles/${MACHINE}/${ONLPV}"
ONIEIMAGE_MACHINE_CONF ?= "${ONIEIMAGE_CONF_DIR}/machine.conf"
ONIEIMAGE_PLATFORM_CONF ?= "${ONIEIMAGE_CONF_DIR}/platform.conf"

ONIEIMAGE_PAYLOAD_FILE = "${ONIEIMAGE_DIR}/onie_install.tar"
ONIEIMAGE_OUTPUT_FILE = "${ONIEIMAGE_DIR}/onie-installer.bin"

FIT_HASH_ALG = "sha1"

IMAGE_FSTYPES += "tar.bz2 ext4"

python () {
        d.delVarFlag("do_fetch", "noexec")
        d.delVarFlag("do_unpack", "noexec")
        d.delVarFlag("do_image_complete", "noexec")
}

do_bundle () {
    mkdir --parent "${ONIEIMAGE_DIR_INSTALL}"

    # Install script and install file depend on the ARCH
    if [ "${TARGET_ARCH}" = "x86_64" ]; then
        INSTALL_SCRIPT="${WORKDIR}/grub-env/install.sh"
        cp "${INSTALL_FILE}" "${ONIEIMAGE_DIR_INSTALL}/rootfs.tar.xz"
    elif [ "${TARGET_ARCH}" = "arm" ]; then
        INSTALL_SCRIPT="${WORKDIR}/u-boot-env/install.sh"
        cp "${INSTALL_FILE}" "${ONIEIMAGE_DIR_INSTALL}/fitImage-mion.bin"
    else
        bbfatal "Unable to parse TARGET_ARCH"
    fi

    # Generate the payload tar file
    cp "${INSTALL_SCRIPT}" "${ONIEIMAGE_PLATFORM_CONF}" "${ONIEIMAGE_MACHINE_CONF}" "${ONIEIMAGE_DIR_INSTALL}"
    # Set the mion version in the grub menu
    sed -i -e "s/MION_VERSION=/MION_VERSION=\"${DISTRO_VERSION}\"/" "${ONIEIMAGE_DIR_INSTALL}/install.sh"
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

IMAGE_POSTPROCESS_COMMAND_append=" do_bundle; "

