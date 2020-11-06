# Copyright (C) 2018 Tobias Jungel <tobias.jungel@bisdn.de>
# Released under the MIT license (see COPYING.MIT for the terms)

# SPDX-License-Identifier: MIT

DESCRIPTION = "Open Network Linux Components for Yocto integration"
HOMEPAGE = "https://www.opennetlinux.org/"
LICENSE = "EPLv1.0"
LIC_FILES_CHKSUM = "\
  file://LICENSE;beginline=14;md5=7e6d108802170df125adb4f452c3c9dd \
  file://${SUBMODULE_INFRA}/LICENSE;md5=457079d296746aac524eb56eb6822ea8 \
  file://${SUBMODULE_BIGCODE}/LICENSE;md5=dc6bd4d967e085fe783aa2abe7655c60 \
"

SRCREV_onl ?= "${AUTOREV}"
SRCREV_infra ?= "16ce9cd77f6639aac4813d698f9dd11f3ee47e7a"
SRCREV_bigcode ?= "94091600faa76fac492b4be2ffab17cb9788d697"

URI_ONL = "git://github.com/opencomputeproject/OpenNetworkLinux.git;protocol=https;branch=ONLPv2"
URI_INFRA = "git://github.com/floodlight/infra.git"
URI_BIGCODE = "git://github.com/floodlight/bigcode.git"

SRCREV_FORMAT = "onl_infra_bigcode"
EXTRA_OEMAKE = "\
    'SUBMODULE_INFRA=${S}/sm/infra'\
"

ONLP_VERSION = "onlpv2"

# submodules are checked out individually to support license file checking
SRC_URI = "${URI_ONL};name=onl \
           ${URI_INFRA};name=infra;destsuffix=git/${SUBMODULE_INFRA} \
           ${URI_BIGCODE};name=bigcode;destsuffix=git/${SUBMODULE_BIGCODE} \
           file://ar.patch;patchdir=${SUBMODULE_INFRA} \
           file://onlpdump.service \
           file://gcc-strncpy-fix.patch \
           file://0002-fix-Werror-unused-result.patch \
           file://0001-i2c-bigcode-use-libi2c-for-onlpdump-and-update-headers.patch;patchdir=${SUBMODULE_BIGCODE} \
           file://0001-i2c-infra-use-libi2c-for-onlpdump-and-update-headers.patch;patchdir=${SUBMODULE_INFRA} \
           file://0001-i2c-use-libi2c-for-onlpdump-and-update-headers.patch \
           file://0001-Add-smbus-header-to-onlp-i2c-source.patch \
"

inherit systemd

SYSTEMD_SERVICE_${PN} = "onlpdump.service"
SYSTEMD_AUTO_ENABLE = "enable"

inherit pythonnative

DEPENDS = "i2c-tools python libedit libzip"

S = "${WORKDIR}/git"
PV = "1.1+git${SRCPV}"

PACKAGE_ARCH = "${MACHINE_ARCH}"
PROVIDES += "libonlp2 libonlp2-platform libonlp2-platform-defaults"
INSANE_SKIP_${PN} = "file-rdeps"

#### TODO onl.bbclass?
ONL = "${S}"

SUBMODULE_INFRA = "sm/infra"
SUBMODULE_BIGCODE = "sm/bigcode"
BUILDER = "${S}/${SUBMODULE_INFRA}/builder/unix"

BUILDER_MODULE_DATABASE = "${WORKDIR}/modules/modules.json"
BUILDER_MODULE_DATABASE_ROOT = "${WORKDIR}"
BUILDER_MODULE_MANIFEST = "${WORKDIR}/modules/modules.mk"
MODULEMANIFEST = "${BUILDER_MODULE_MANIFEST}"

#export SUBMODULE_INFRA BUILDER BUILDER_MODULE_DATABASE BUILDER_MODULE_DATABASE_ROOT BUILDER_MODULE_MANIFEST MODULEMANIFEST ONL
ARCH = "${TARGET_ARCH}"
TOOLCHAIN = "gcc-local"
NO_USE_GCC_VERSION_TOOL="1"

###
# TODO CFLAGS?
EXTRA_OEMAKE = "\
  'AR=${AR}' \
  'ARCH=${ARCH}' \
  'BUILDER=${BUILDER}' \
  'BUILDER_MODULE_DATABASE=${BUILDER_MODULE_DATABASE}' \
  'BUILDER_MODULE_DATABASE_ROOT=${BUILDER_MODULE_DATABASE_ROOT}' \
  'BUILDER_MODULE_MANIFEST=${BUILDER_MODULE_MANIFEST}' \
  'MODULEMANIFEST=${MODULEMANIFEST}' \
  'GCC=${CC}' \
  'GCC_FLAGS=${CFLAGS}' \
  'MODULEMANIFEST=${MODULEMANIFEST}' \
  'NO_USE_GCC_VERSION_TOOL=${NO_USE_GCC_VERSION_TOOL}' \
  'ONL=${ONL}' \
  'ONL_DEBIAN_SUITE=${ONL_DEBIAN_SUITE}' \
  'SUBMODULE_BIGCODE=${SUBMODULE_BIGCODE}' \
  'SUBMODULE_INFRA=${SUBMODULE_INFRA}' \
  'SUBMODULE_INFRA=${SUBMODULE_INFRA}' \
  'TOOLCHAIN=${TOOLCHAIN}' \
"

do_configure() {
  mkdir -p $(dirname ${BUILDER_MODULE_DATABASE})
  MODULEMANIFEST_=$(${BUILDER}/tools/modtool.py --db ${BUILDER_MODULE_DATABASE} --dbroot ${BUILDER_MODULE_DATABASE_ROOT} --make-manifest ${BUILDER_MODULE_MANIFEST})
  # XXX compare MODULEMANIFEST_ vs MODULEMANIFEST
  echo ${MODULEMANIFEST_}
}

do_compile() {
  V=1 VERBOSE=1 oe_runmake -C packages/base/any/onlp/builds alltargets
  V=1 VERBOSE=1 oe_runmake -C packages/base/any/onlp/builds/onlpd alltargets
  V=1 VERBOSE=1 oe_runmake -C packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONIE_MACHINE}/onlp/builds alltargets
}

do_install() {
  # folders in dest
  install -d \
    ${D}${bindir} \
    ${D}${includedir}/AIM \
    ${D}${includedir}/BigList \
    ${D}${includedir}/IOF \
    ${D}${includedir}/cjson \
    ${D}${includedir}/onlp \
    ${D}${includedir}/onlplib \
    ${D}${libdir} \
    ${D}/usr/lib/python2.7/site-packages \
    ${D}/usr/lib/python2.7/site-packages/onlp \
    ${D}/usr/lib/python2.7/site-packages/onlp/onlp \
    ${D}/usr/lib/python2.7/site-packages/onlp/test \
    ${D}/usr/lib/python2.7/site-packages/onlp/onlplib \
    ${D}/usr/lib/python2.7/site-packages/onlp/sff \ 
    ${D}/usr/lib/python2.7/site-packages/AIM \
    ${D}/usr/lib/python2.7/site-packages/BigList \
    ${D}/usr/lib/python2.7/site-packages/cjson_util \


  # install onlpdump
  install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONIE_MACHINE}/onlp/builds/onlps/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/onlps ${D}${bindir}

  # install onlpdump.py and libs
  install -m 0755 packages/base/any/onlp/src/onlpdump.py ${D}${bindir}
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/__init__.py ${D}/${libdir}/python2.7/site-packages/onlp/
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/onlp/* ${D}/${libdir}/python2.7/site-packages/onlp/onlp/
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/test/* ${D}/${libdir}/python2.7/site-packages/onlp/test/
  install -m 0755 packages/base/any/onlp/src/onlplib/module/python/onlp/onlplib/* ${D}/${libdir}/python2.7/site-packages/onlp/onlplib/
  install -m 0755 sm/bigcode/modules/sff/module/python/sff/* ${D}/${libdir}/python2.7/site-packages/onlp/sff

  install -m 0755 packages/base/any/onlp/src/onlp/module/python/AIM/* ${D}/${libdir}/python2.7/site-packages/AIM
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/BigList/* ${D}/${libdir}/python2.7/site-packages/BigList
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/cjson_util/* ${D}/${libdir}/python2.7/site-packages/cjson_util

  # install headers
  install -m 0644 packages/base/any/onlp/src/onlp/module/inc/onlp/*.h ${D}${includedir}/onlp/
  install -m 0644 packages/base/any/onlp/src/onlplib/module/inc/onlplib/*.h ${D}${includedir}/onlplib/
  install -m 0644 sm/bigcode/modules/BigData/BigList/module/inc/BigList/*.h ${D}${includedir}/BigList/
  install -m 0644 sm/bigcode/modules/IOF/module/inc/IOF/*.h ${D}${includedir}/IOF/
  install -m 0644 sm/bigcode/modules/cjson/module/inc/cjson/*.h ${D}${includedir}/cjson/
  install -m 0644 sm/infra/modules/AIM/module/inc/AIM/*.h ${D}${includedir}/AIM/

  # install libonlp-platform shared library (includes AIM.a  AIM_posix.a  BigList.a  cjson.a  cjson_util.a  IOF.a  onlplib.a  x86_64_delta_ag7648.a)
  #
  install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONIE_MACHINE}/onlp/builds/lib/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so ${D}${libdir}
  mv ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so.1
  ln -r -s ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so.1 ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so
  ln -r -s ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so.1 ${D}${libdir}/libonlp-platform.so.1

  # install libonlp shared library (includes TODO)
  install -m 0755 packages/base/any/onlp/builds/onlp/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp.so ${D}${libdir}
  mv ${D}${libdir}/libonlp.so ${D}${libdir}/libonlp.so.1
  ln -r -s ${D}${libdir}/libonlp.so.1 ${D}${libdir}/libonlp.so

  # install libonlp shared library (includes TODO)
  install -m 0755 packages/base/any/onlp/builds/onlp-platform-defaults/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp-platform-defaults.so ${D}${libdir}
  mv ${D}${libdir}/libonlp-platform-defaults.so ${D}${libdir}/libonlp-platform-defaults.so.1
  ln -r -s ${D}${libdir}/libonlp-platform-defaults.so.1 ${D}${libdir}/libonlp-platform-defaults.so

  # platform file
  install -d ${D}${sysconfdir}/onl
  echo "${ONL_ARCH}-${ONIE_VENDOR}-${ONIE_MACHINE}-r${ONIE_MACHINE_REV}" > ${D}${sysconfdir}/onl/platform

  # service file
  install -d ${D}${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/onlpdump.service ${D}${systemd_unitdir}/system
  sed -i -e 's,@BINDIR@,${bindir},g' \
         ${D}${systemd_unitdir}/system/*.service
}

FILES_${PN} = "${libdir}/python2.7 \ 
    ${sysconfdir} \
    ${bindir} \
    ${includedir}/AIM \
    ${includedir}/BigList \
    ${includedir}/IOF \
    ${includedir}/cjson \
    ${includedir}/onlp \
    ${includedir}/onlplib \
    ${libdir} \
    ${libdir}/python2.7/site-packages/* \
"

