SUMMARY = "Create a local feed of Mion guest images to allow offline creation of guests"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://01-local-feed.conf"

python do_compile() {
    import json
    import shutil

    b_dir = d.getVar('B')
    mion_output_dir = d.getVar('MION_OUTPUT_DIR')
    machine = d.getVar('MACHINE')
    image_list = (d.getVar('MION_LOCAL_FEED_IMAGES') or '').split()

    if os.path.exists(b_dir):
        shutil.rmtree(b_dir)

    for image in image_list:
        (system_profile, application_profile) = image.split(':')
        src_dir = os.path.join(mion_output_dir, machine, system_profile, application_profile)
        image_json_src_path = os.path.join(src_dir, 'image_guest.json')

        with open(image_json_src_path, 'r') as f:
            image_config = json.load(f)

        rootfs_fname = image_config['ROOTFS']
        rootfs_src_path = os.path.join(src_dir, rootfs_fname)

        dest_dir = os.path.join(b_dir, application_profile)
        image_json_dest_path = os.path.join(dest_dir, 'image_guest.json')
        rootfs_dest_path = os.path.join(dest_dir, rootfs_fname)

        os.makedirs(dest_dir, exist_ok=True)
        oe.path.copyhardlink(image_json_src_path, image_json_dest_path)
        oe.path.copyhardlink(rootfs_src_path, rootfs_dest_path)
}

do_install() {
    install -d -m 0755 "${D}${datadir}/mion/local-feed/guest"
    install -d -m 0755 "${D}${datadir}/mion/preconfig.d"
    install -m 0644 "${WORKDIR}/01-local-feed.conf" "${D}${datadir}/mion/preconfig.d"

    # The install command sadly has no recursive mode
    find . -type d -exec install -d -m 0755 "{}" "${D}${datadir}/mion/local-feed/guest/{}" \;
    find . -type f -exec install -m 0644 "{}" "${D}${datadir}/mion/local-feed/guest/{}" \;
}

FILES_${PN} = "${datadir}/mion"
