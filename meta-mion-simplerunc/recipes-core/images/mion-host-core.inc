SUMMARY = "mion universal host image recipe."
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    openssh-sshd \
    openssh-sftp-server \
    openssh-scp \
    os-release \
    runc-opencontainers \
    netns \
    iproute2 \
    iptables \
    cgroup-lite \
    ca-certificates \
    openssh-ssh \
    bash \
    e2fsprogs-resize2fs \
    e2fsprogs-tune2fs \
    grub \
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
    ${CONTAINER_DEPENDS} \
    preconfig-guests \
    mion-local-feed \
"
IMAGE_FEATURES += "ssh-server-openssh"

IMAGE_FEATURES_remove = "doc-pkgs"

