# SPDX-License-Identifier: MIT

SUMMARY = "mion host image recipe."
LICENSE = "MIT"

require mion-host-core.inc

IMAGE_INSTALL += " \
    srunc \
    preconfig-guests \
"


ROOTFS_POSTPROCESS_COMMAND += "rootfs_install_container ; rootfs_mion_local_feed ; rootfs_install_localfeed ;"

rootfs_install_container () {
 for x in ${CONTAINER_NAMES}; do
    install -d ${IMAGE_ROOTFS}/usr/share/srunc/local-feed/guest/${x}
    install ${TOPDIR}/tmp-guest-${MACHINE}-${TCLIBC}/deploy/images/${MACHINE}/${x}-${MACHINE}.tar.xz ${IMAGE_ROOTFS}/usr/share/srunc/local-feed/guest/${x}/
 done
}


python rootfs_mion_local_feed() {
    import json
    import shutil

    b_dir = d.getVar('B')
    deploy_dir = d.getVar('DEPLOY_DIR')
    machine = d.getVar('MACHINE')
    image_list = (d.getVar('CONTAINER_NAMES') or '').split()

    for image in image_list:
        src_dir="{}/tmp-guest-{}-{}/deploy/images/{}".format(d.getVar('TOPDIR'), d.getVar('MACHINE'), d.getVar('TCLIBC'), d.getVar('MACHINE'))
        json_file_name="image_{}-{}.json".format(image, d.getVar('MACHINE'))
        image_json_src_path = os.path.join(src_dir, json_file_name)

        with open(image_json_src_path, 'r') as f:
            image_config = json.load(f)

        rootfs_fname = image_config['ROOTFS']
        rootfs_src_path = os.path.join(src_dir, rootfs_fname)
        rootfs=d.getVar('IMAGE_ROOTFS', True)
        dest_dir = "{}{}{}".format(rootfs, "/usr/share/srunc/local-feed/guest/", image)
        image_json_dest_path = os.path.join(dest_dir, 'image_guest.json')
        rootfs_dest_path = os.path.join(dest_dir, rootfs_fname)
        os.makedirs(dest_dir, exist_ok=True)
        try:
            oe.path.copyhardlink(image_json_src_path, image_json_dest_path)
        except:
            pass

        try:
            oe.path.copyhardlink(rootfs_src_path, rootfs_dest_path)
        except:
            pass
}

rootfs_install_localfeed () {
    install -d -m 0755 "${D}${datadir}/srunc/local-feed/guest"
    install -d -m 0755 "${D}${datadir}/srunc/preconfig.d"
    echo "[source:local]" >> "${D}${datadir}/srunc/preconfig.d/01-local-feed.conf"
    echo "url = file:///usr/share/srunc/local-feed" >> "${D}${datadir}/srunc/preconfig.d/01-local-feed.conf"
    chmod 644 "${D}${datadir}/srunc/preconfig.d/01-local-feed.conf"
}


