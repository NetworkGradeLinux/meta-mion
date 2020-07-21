#inherit allarch
SUMMARY = "Create ONIE based mion image"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
INHIBIT_DEFAULT_DEPS = "1"
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
SRC_URI = "file://grub-env/install.sh \
           file://sharch_body.sh \
	   "
do_configure[noexec] = "1"
do_install[noexec] = "1"
do_package[noexec] = "1"
do_packagedata[noexec] = "1"
do_package_write_ipk[noexec] = "1"
do_package_write_rpm[noexec] = "1"
do_package_write_deb[noexec] = "1"
do_populate_sysroot[noexec] = "1"

ONIEIMAGE_DIR ?= "${S}"
ONIEIMAGE_DIR_INSTALL = "${ONIEIMAGE_DIR}/installer/"

ONIEIMAGE_SHELL_ARCHIVE_BODY ?= "${WORKDIR}/sharch_body.sh"
ONIEIMAGE_INSTALL_SCRIPT ?= "${WORKDIR}/grub-env/install.sh"
ONIEIMAGE_CONF_DIR ?= "${MIONBASE}/meta-mion-${ONIE_VENDOR}/conf/onie-profiles/${MACHINE}/${ONLPV}"
ONIEIMAGE_MACHINE_CONF ?= "${ONIEIMAGE_CONF_DIR}/machine.conf"
ONIEIMAGE_PLATFORM_CONF ?= "${ONIEIMAGE_CONF_DIR}/platform.conf"

ONIEIMAGE_PAYLOAD_FILE = "${ONIEIMAGE_DIR}/onie_install.tar"
ONIEIMAGE_OUTPUT_FILE = "${ONIEIMAGE_DIR}/onie-installer.bin"

do_compile() {
    mkdir --parent "${ONIEIMAGE_DIR_INSTALL}"
    # Generate the payload tar file
    cp "${ONIEIMAGE_INSTALL_SCRIPT}" "${ONIEIMAGE_PLATFORM_CONF}" "${ONIEIMAGE_MACHINE_CONF}" "${ONIEIMAGE_DIR_INSTALL}"
    #FIXME for arm machines the bzImage should be a fitImage containing DTB etc.
    #cp "${DEPLOY_DIR_IMAGE}/bzImage" "${ONIEIMAGE_DIR_INSTALL}/bzImage"
    cp "${DEPLOY_DIR_IMAGE}/mion-image-${ONLPV}-${MACHINE}.tar.xz" "${ONIEIMAGE_DIR_INSTALL}/rootfs.tar.xz"
    tar -C "${ONIEIMAGE_DIR}" -cf "${ONIEIMAGE_PAYLOAD_FILE}" "installer"

    # Populate the shell archive body
    cp "${ONIEIMAGE_SHELL_ARCHIVE_BODY}" "${ONIEIMAGE_OUTPUT_FILE}"
    image_sha1=$(sha1sum "${ONIEIMAGE_PAYLOAD_FILE}" | awk '{print $1}')
    sed -i -e "s/%%IMAGE_SHA1%%/${image_sha1}/" "${ONIEIMAGE_OUTPUT_FILE}"
    #TODO add the build date to the shell archive
    #build_date=$(date +%Y-%m-%d)
    #sed -i -e "s/%%BUILD_DATE%%/${build_date}/" "${ONIEIMAGE_OUTPUT_FILE}"

    # Generate the ONIE bin file
    cat "${ONIEIMAGE_PAYLOAD_FILE}" >> "${ONIEIMAGE_OUTPUT_FILE}"
}

do_compile[depends] = "mion-image-${ONLPV}:do_image_complete"
#image-json-file:do_build"

inherit deploy

do_deploy() {
    . ${ONIEIMAGE_MACHINE_CONF}
    install -d ${DEPLOYDIR}
    install -m 0644 -T ${ONIEIMAGE_OUTPUT_FILE} ${DEPLOY_DIR_IMAGE}/onie-installer-${PLATFORM}.bin
    ln -sf ${DEPLOY_DIR_IMAGE}/onie-installer-${PLATFORM}.bin ${DEPLOY_DIR_IMAGE}/onie-installer.bin
}

addtask do_deploy after do_compile before do_build
