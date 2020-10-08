# SPDX-License-Identifier: MIT

inherit allarch

SUMMARY = "Create Mion image.json file"
DESCRIPTION = "Create an image.json file used by Mion to fetch and install additional guest images"
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

python do_compile() {
    import json

    system_profile_type = d.getVar('MION_SYSTEM_PROFILE_TYPE')

    if system_profile_type not in ["native", "guest"]:
        bb.fatal("MION_SYSTEM_PROFILE_TYPE must be set to \"native\" or \"guest\" in the system profile config")

    image = {}
    image['MACHINE'] = d.getVar('MACHINE')
    image['DISTRO'] = d.getVar('DISTRO')
    image['VERSION'] = d.getVar('DISTRO_VERSION')
    image['SYSTEM_PROFILE'] = d.getVar('MION_SYSTEM_PROFILE')
    image['SYSTEM_PROFILE_TYPE'] = system_profile_type
    image['APPLICATION_PROFILE'] = d.getVar('MION_APPLICATION_PROFILE')
    image['ROOTFS'] = d.getVar('MION_ROOTFS_IMAGE')

    if system_profile_type == 'native':
        image['KERNEL'] = d.getVar('MION_KERNEL_IMAGE')
    elif system_profile_type == 'guest':
        image['COMMAND'] = d.getVar('MION_APPLICATION_COMMAND')
        image['CAPABILITIES'] = d.getVar('MION_GUEST_CAPABILITIES').split()

    image_file_name = "image_{}.json".format(system_profile_type)
    with open(image_file_name, 'w') as fp:
        json.dump(image, fp, indent=4, sort_keys=True)
        fp.write('\n')
}

inherit deploy

do_deploy() {
    install -d ${DEPLOYDIR}
    install -m 0644 image_${MION_SYSTEM_PROFILE_TYPE}.json ${DEPLOYDIR}
}

addtask do_deploy after do_compile before do_build
