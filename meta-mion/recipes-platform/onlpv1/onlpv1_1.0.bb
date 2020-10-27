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

SRCREV_onl ?= "01c689605d626372ad8a1b0fbab8687e9705efbe"
SRCREV_infra ?= "168b695e51241be2823111f105b129236a1d79f8"
SRCREV_bigcode ?= "7294ff56e750c188d1f3b074ffbadd2024d50089"

URI_ONL ?= "git://github.com/opencomputeproject/OpenNetworkLinux.git;protocol=https"
URI_INFRA ?= "git://github.com/floodlight/infra.git"
URI_BIGCODE ?= "git://github.com/floodlight/bigcode.git"

SRCREV_FORMAT = "onl_infra_bigcode"

# submodules are checked out individually to support license file checking
SRC_URI = "${URI_ONL};name=onl \
           ${URI_INFRA};name=infra;destsuffix=git/${SUBMODULE_INFRA} \
           ${URI_BIGCODE};name=bigcode;destsuffix=git/${SUBMODULE_BIGCODE} \
           file://onlpdump.service \
           file://gcc-strncpy-fix.patch \
           file://ar.patch;patchdir=${SUBMODULE_INFRA} \
           file://0002-fix-Werror-unused-result.patch \
"

inherit systemd

require onlpv1.inc

SYSTEMD_SERVICE_${PN} = "onlpdump.service"
SYSTEMD_AUTO_ENABLE = "enable"

DEPENDS = "i2c-tools python libedit libzip"

S = "${WORKDIR}/git"
PV = "1.1+git${SRCPV}"

PACKAGE_ARCH = "${MACHINE_ARCH}"
PROVIDES += "libonlp libonlp-platform libonlp-platform-defaults"
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

ARCH = "${TARGET_ARCH}"
TOOLCHAIN = "gcc-local"
NO_USE_GCC_VERSION_TOOL="1"

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
    ${D}/${libdir}/python${PYTHON_MAJMIN}/ \
    ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp \
    ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlp \
    ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/test \
    ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlplib \
    ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/sff 

  # install onlpdump
  install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONIE_MACHINE}/onlp/builds/onlpdump/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/onlps ${D}${bindir}

  # install onlpdump.py and libs
  install -m 0755 packages/base/any/onlp/src/onlpdump.py ${D}${bindir}
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/__init__.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/onlp/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlp/
  install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/test/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/test/
  install -m 0755 packages/base/any/onlp/src/onlplib/module/python/onlp/onlplib/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlplib/
  install -m 0755 sm/bigcode/modules/sff/module/python/sff/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/sff

  # install headers
  install -m 0644 packages/base/any/onlp/src/onlp/module/inc/onlp/*.h ${D}${includedir}/onlp/
  install -m 0644 packages/base/any/onlp/src/onlplib/module/inc/onlplib/*.h ${D}${includedir}/onlplib/
  install -m 0644 sm/bigcode/modules/BigData/BigList/module/inc/BigList/*.h ${D}${includedir}/BigList/
  install -m 0644 sm/bigcode/modules/IOF/module/inc/IOF/*.h ${D}${includedir}/IOF/
  install -m 0644 sm/bigcode/modules/cjson/module/inc/cjson/*.h ${D}${includedir}/cjson/
  install -m 0644 sm/infra/modules/AIM/module/inc/AIM/*.h ${D}${includedir}/AIM/

  # install libonlp-platform shared library (includes AIM.a  AIM_posix.a  BigList.a  cjson.a  cjson_util.a  IOF.a  onlplib.a  x86_64_delta_ag7648.a)
  #
  install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONIE_MACHINE}/onlp/builds/lib/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp-${ONIE_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so ${D}${libdir}
  mv ${D}${libdir}/libonlp-${ONIE_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so ${D}${libdir}/libonlp-${ONIE_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so.1
  ln -r -s ${D}${libdir}/libonlp-${ONIE_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so.1 ${D}${libdir}/libonlp-${ONIE_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so
  ln -r -s ${D}${libdir}/libonlp-${ONIE_ARCH}-${ONL_VENDOR}-${ONIE_MACHINE}.so.1 ${D}${libdir}/libonlp-platform.so.1

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
  echo "${ONIE_ARCH}-${ONIE_VENDOR}-${ONIE_MACHINE}-r${ONIE_MACHINE_REV}" > ${D}${sysconfdir}/onl/platform

  # service file
  install -d ${D}${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/onlpdump.service ${D}${systemd_unitdir}/system
  sed -i -e 's,@BINDIR@,${bindir},g' \
         ${D}${systemd_unitdir}/system/*.service

  # install platform.xml file
  install -d ${D}/lib/platform-config/current/onl/
  install -m 0664 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONIE_MACHINE}/platform-config/r0/src/lib/platform.xml ${D}/lib/platform-config/current/onl/platform.xml
}

FILES_${PN} = "${libdir}/python${PYTHON_MAJMIN} \ 
    ${sysconfdir} \
    ${bindir} \
    ${includedir}/AIM \
    ${includedir}/BigList \
    ${includedir}/IOF \
    ${includedir}/cjson \
    ${includedir}/onlp \
    ${includedir}/onlplib \
    ${libdir} \
    ${libdir}/python${PYTHON_MAJMIN}/ \
    ${libdir}/python${PYTHON_MAJMIN}/onlp \
    ${libdir}/python${PYTHON_MAJMIN}/onlp/onlp \
    ${libdir}/python${PYTHON_MAJMIN}/onlp/test \
    ${libdir}/python${PYTHON_MAJMIN}/onlp/onlplib \
    ${libdir}/python${PYTHON_MAJMIN}/onlp/sff \
    /lib/platform-config/ \
"
