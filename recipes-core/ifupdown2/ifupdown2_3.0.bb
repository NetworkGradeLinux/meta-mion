SUMMARY = "interface network manager"
HOMEPAGE = "https://github.com/CumulusNetworks/ifupdown2"
LICENSE = "GPL-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=8c16666ae6c159876a0ba63099614381 \
                    file://debian/copyright;md5=03883f7849f14a9d540cf8cea2cc078d"

SRC_URI = "git://github.com/CumulusNetworks/ifupdown2.git;protocol=https"

# Modify these as desired
PV = "3.0.0+git${SRCPV}"
SRCREV = "7cc5525d2259f17c7b75439812c78678bb85708a"

S = "${WORKDIR}/git"

inherit setuptools3

RDEPENDS_${PN} += "bash python3-core python3-crypt python3-io python3-json python3-logging python3-netserver python3-pickle python3-pprint python3-resource python3-shell python3-stringold python3-threading"


