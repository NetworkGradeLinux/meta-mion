SUMMARY = "Create a local feed of srunc guest images to allow offline creation of guests"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://01-local-feed.conf"

do_compile[nostamp]="1"
do_install[nostamp]="1"
do_compile[depends] = "${CDEPS}"

python () {
    import os
    tmp=d.getVar('CONTAINER_NAMES', True).split(' ')
    if not tmp:
        raise bb.parse.SkipRecipe("No containers specified in CONTAINER_NAMES.")
    cdeps=""
    for i, s in enumerate(tmp):
        if s is not "":
            cdeps=cdeps + s +":do_image_complete "
    d.setVar("CDEPS", cdeps.rstrip().lstrip())
}


python do_compile() {
    import json
    import shutil

    b_dir = d.getVar('B')
    deploy_dir = d.getVar('DEPLOY_DIR')
    machine = d.getVar('MACHINE')
    image_list = (d.getVar('CONTAINER_NAMES') or '').split()

    for image in image_list:
        src_dir = d.getVar('TOPDIR') + "/tmp/deploy/images/" + d.getVar('MACHINE')
        json_file_name="image_{}-{}.json".format(image, d.getVar('MACHINE'))
        image_json_src_path = os.path.join(src_dir, json_file_name)

        with open(image_json_src_path, 'r') as f:
            image_config = json.load(f)

        rootfs_fname = image_config['ROOTFS']
        rootfs_src_path = os.path.join(src_dir, rootfs_fname)

        dest_dir = os.path.join(b_dir, image)
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

do_install() {
    install -d -m 0755 "${D}${datadir}/srunc/local-feed/guest"
    install -d -m 0755 "${D}${datadir}/srunc/preconfig.d"
    install -m 0644 "${WORKDIR}/01-local-feed.conf" "${D}${datadir}/srunc/preconfig.d"

    # The install command sadly has no recursive mode
    find . -type d -exec install -d -m 0755 "{}" "${D}${datadir}/srunc/local-feed/guest/{}" \;
    find . -type f -exec install -m 0644 "{}" "${D}${datadir}/srunc/local-feed/guest/{}" \;
}

FILES_${PN} = "${datadir}/srunc"
