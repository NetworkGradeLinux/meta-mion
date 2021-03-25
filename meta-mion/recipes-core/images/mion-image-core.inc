SUMMARY = "mion universal base image recipe."
LICENSE = "MIT"

inherit core-image

# Package groups
IMAGE_INSTALL_append = " \
    packagegroup-core-boot-mion \
    packagegroup-core-full-cmdline \
    ${@bb.utils.contains('MACHINE_FEATURES', 'tpm2', 'packagegroup-security-tpm2', '', d)} \
"

# Individual packages not in the packagegroups
IMAGE_INSTALL_append = " \
    bind-utils \
    bridge-utils \
    curl \
    efibootmgr \
    findutils \
    glibc \
    haveged \
    htop \
    i2c-tools \
    ipmitool \
    kernel-modules \
    onie-tools \
    openssh-ssh \
    openssh-sshd \
    os-release \
    parted \
    pciutils \
    rsync \
    tmux \
    tree \
    tzdata \
    usbutils \
    util-linux-blkid \
    vim \
    which \
    xz \
"

IMAGE_FEATURES += "ssh-server-openssh dev-pkgs"

IMAGE_FEATURES_remove = "doc-pkgs"

PREFERRED_PROVIDER_virtual/base-utils = "packagegroup-core-base-utils"
VIRTUAL-RUNTIME_base-utils = "packagegroup-core-base-utils"
VIRTUAL-RUNTIME_base-utils-hwclock = "util-linux-hwclock"
VIRTUAL-RUNTIME_base-utils-syslog = ""
