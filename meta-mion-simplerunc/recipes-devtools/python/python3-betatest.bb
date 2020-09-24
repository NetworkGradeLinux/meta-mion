DESCRIPTION = "Testing helpers for Python 3.6+"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d229da563da18fe5d58cd95a6467d584"

PV = "0.1.0"
SRC_URI[md5sum] = "9692bd9bdd6c8df0979e6bf7c45b1b2d"
SRC_URI[sha256sum] = "66a17932edd501aa6492262daf7cbd5c86d1bae05bafa4a218fc207cf07bb1e6"

inherit setuptools3 pypi

RDEPENDS_${PN} = " \
    ${PYTHON_PN}-unittest \
    "
