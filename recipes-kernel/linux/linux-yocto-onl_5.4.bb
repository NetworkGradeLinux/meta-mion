KBRANCH ?= "linux-5.4.y"

require linux-yocto-onl.inc

LIC_FILES_CHKSUM = "file://COPYING;md5=bbea815ee2795b2f4230826c0c6b8814"

LINUX_VERSION ?= "5.4.34"
#https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git/log/?h=linux-5.4.y
SRCREV_machine ?= "6ccc74c083c0d472ac64f3733f5b7bf3f99f261e"
SRCREV_meta ?= "054d410d1a73e4729f1fe7540db058de69fe8cfe"

SRC_URI += "\
    git://git.yoctoproject.org/yocto-kernel-cache;type=kmeta;name=meta;branch=yocto-5.4;destsuffix=kernel-meta \
    file://bisdn-kmeta;type=kmeta;name=bisdn-kmeta;destsuffix=bisdn-kmeta \
"
