SUMMARY = "This module supports dps850"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=12f884d2ae1ff87c09e5b7ccc2c4ca7e"

inherit module

SRC_URI = " \
	  file://Makefile \
          file://dps850.c \
	  file://COPYING \
          "

S = "${WORKDIR}"

do_install() {
    install -d ${D}/lib/modules/${KERNEL_VERSION}/onl
    install -m 0644 dps850.ko ${D}/lib/modules/${KERNEL_VERSION}/onl
}

#KERNEL_MODULE_AUTOLOAD += " dps850"
