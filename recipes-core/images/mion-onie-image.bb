inherit allarch

SUMMARY = "Create ONIE based Oryx image"
LICENSE = "MIT"
INHIBIT_DEFAULT_DEPS = "1"

do_fetch[noexec] = "1"
do_unpack[noexec] = "1"
do_patch[noexec] = "1"
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

ONIEIMAGE_SHELL_ARCHIVE_BODY ?= "${ONIE_FILES}/onie/sharch_body.sh"
ONIEIMAGE_INSTALL_SCRIPT ?= "${ONIE_FILES}/onie/grub-env/install.sh"
ONIEIMAGE_CONF_DIR ?= "${ORYX_BASE}/meta-mion-${ONIE_VENDOR}/conf/onie-profiles/${ONIE_MACHINE_TYPE}/${ONLP_VERSION}"
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
    cp "${DEPLOY_DIR_IMAGE}/${DISTRO}-${ORYX_SYSTEM_PROFILE}-${ORYX_APPLICATION_PROFILE}-${MACHINE}.tar.xz" "${ONIEIMAGE_DIR_INSTALL}/rootfs.tar.xz"
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

do_compile[depends] = "oryx-image:do_image_complete image-json-file:do_build"

inherit deploy

do_deploy() {
    . ${ONIEIMAGE_MACHINE_CONF}
    install -d ${DEPLOYDIR}
    install -m 0644 -T ${ONIEIMAGE_OUTPUT_FILE} ${DEPLOY_DIR_IMAGE}/onie-installer-${PLATFORM}.bin
    ln -s ${DEPLOY_DIR_IMAGE}/onie-installer-${PLATFORM}.bin ${DEPLOY_DIR_IMAGE}/onie-installer.bin
}

addtask do_deploy after do_compile before do_build
