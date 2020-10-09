# SPDX-License-Identifier: MIT

inherit allarch

SUMMARY = "Create srunc image_guest.json file"
DESCRIPTION = "Create an image_guest.json file used by Srunc to fetch and install additional guest images"
LICENSE = "MIT"
INHIBIT_DEFAULT_DEPS = "1"

do_fetch[noexec] = "1"
do_unpack[noexec] = "1"
do_patch[noexec] = "1"
do_compile[nostamp] = "1"
do_configure[noexec] = "1"
do_install[noexec] = "1"
do_package[noexec] = "1"
do_packagedata[noexec] = "1"
do_package_write_ipk[noexec] = "1"
do_package_write_deb[noexec] = "1"
do_populate_sysroot[noexec] = "1"
do_package_write_ipk[noexec] = "1"
deltask do_package_write_rpm

python do_compile() {
    import json

    image = {}
    image['MACHINE'] = d.getVar('MACHINE')
    image['DISTRO'] = d.getVar('DISTRO')
    image['VERSION'] = d.getVar('DISTRO_VERSION')
    image['ROOTFS'] = "{}-{}.tar.xz".format(d.getVar('IMAGE_BASENAME'), d.getVar('MACHINE'))
    image['COMMAND'] = d.getVar('MION_APPLICATION_COMMAND')
    image['CAPABILITIES'] = d.getVar('MION_GUEST_CAPABILITIES').split()
    json_file_name="image_{}-{}.json".format(d.getVar('IMAGE_BASENAME'), d.getVar('MACHINE'))
    with open(json_file_name, 'w') as fp:
        json.dump(image, fp, indent=4, sort_keys=True)
        fp.write('\n')
}

inherit deploy

do_deploy() {
    install -d ${DEPLOY_DIR_IMAGE}
    install -m 0644 image_${IMAGE_BASENAME}-${MACHINE}.json ${DEPLOY_DIR_IMAGE}
}

addtask do_deploy after do_compile before do_build
