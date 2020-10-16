SUMMARY = "mion minimal container image"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
IMAGE_FSTYPES = "tar.xz"

# Use dumb-init as the init system
VIRTUAL-RUNTIME_init_manager = "dumb-init"
DISTRO_FEATURES_BACKFILL_CONSIDERED = "sysvinit"
ROOTFS_BOOTSTRAP_INSTALL = ""
PREFERRED_PROVIDER_virtual/kernel = "linux-dummy"

inherit core-image

IMAGE_LINGUAS = ""
IMAGE_INSTALL = " \
    dumb-init \
    packagegroup-core-boot \
    openssh-sshd \
    openssh-scp \
    os-release \
    initscripts \
    start-sshd \
    ipmitool \
    os-release \
    iproute2 \
    iptables \
    bash \
    haveged \
    i2c-tools \
    iproute2 \
    less \
    onie-tools \
    parted \
    strace \
    binutils \
    glibc \
    "

MION_APPLICATION_COMMAND = "/sbin/start-sshd"

# Workaround /var/volatile for now
ROOTFS_POSTPROCESS_COMMAND += "rootfs_fixup_var_volatile ;"

rootfs_fixup_var_volatile () {
 install -m 1777 -d ${IMAGE_ROOTFS}/${localstatedir}/volatile/tmp
 install -m 755 -d ${IMAGE_ROOTFS}/${localstatedir}/volatile/log
}

IMAGE_POSTPROCESS_COMMAND += "guest_json; "

guest_json[nostamp]="1"

python guest_json() {
    import json
    image = {}
    image['MACHINE'] = d.getVar('MACHINE')
    image['DISTRO'] = d.getVar('DISTRO')
    image['VERSION'] = d.getVar('DISTRO_VERSION')
    image['ROOTFS'] = "{}-{}.tar.xz".format(d.getVar('IMAGE_BASENAME'), d.getVar('MACHINE'))
    image['COMMAND'] = d.getVar('MION_APPLICATION_COMMAND')
    image['CAPABILITIES'] = d.getVar('MION_GUEST_CAPABILITIES').split()
    json_file_name="image_{}-{}.json".format(d.getVar('IMAGE_BASENAME'), d.getVar('MACHINE'))
    deploy_dir = d.getVar('IMGDEPLOYDIR')

    with open(os.path.join(deploy_dir,json_file_name), 'w+') as fp:
        json.dump(image, fp, indent=4, sort_keys=True)
        fp.write('\n')
}

