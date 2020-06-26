KBRANCH ?= "linux-5.2.y"

require linux-yocto-onl.inc

LIC_FILES_CHKSUM = "file://COPYING;md5=bbea815ee2795b2f4230826c0c6b8814"

LINUX_VERSION ?= "5.2.9"
#https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git/log/?h=linux-5.2.y
SRCREV_machine="aad39e30fb9e6e7212318a1dad898f36f1075648"

SRC_URI += "\
    file://defconfig \
    file://0002-driver-support-intel-igb-bcm5461S-phy.patch \
"

SRC_URI_append = "file://fragment.cfg"
