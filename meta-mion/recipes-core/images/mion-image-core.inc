SUMMARY = "mion universal base image recipe."
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    os-release \
    iproute2 \
    iptables \
    bash \
    e2fsprogs-resize2fs \
    e2fsprogs-tune2fs \
    haveged \
    i2c-tools \
    iproute2 \
    kernel-modules \
    less \
    onie-tools \
    parted \
    strace \
    util-linux \
    util-linux-blkid \
    ipmitool \
    openssh-sshd \
    openssh-ssh \
    binutils \
    glibc \
    tzdata \
    ethtool \
    "

IMAGE_FEATURES += "ssh-server-openssh"

IMAGE_FEATURES_remove = "doc-pkgs"
