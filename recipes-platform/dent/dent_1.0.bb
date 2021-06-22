# Copyright (C) 2018 Tobias Jungel <tobias.jungel@bisdn.de>
# Released under the MIT license (see COPYING.MIT for the terms)

# SPDX-License-Identifier: MIT

DESCRIPTION = "Dent Yocto Integration"
HOMEPAGE = "https://www.dent.dev/"
LICENSE = "EPLv1.0"
LIC_FILES_CHKSUM = "\
  file://LICENSE;beginline=14;md5=eb804cb9780d84f5db1698c0b2fba48d \
  file://${SUBMODULE_INFRA}/LICENSE;md5=457079d296746aac524eb56eb6822ea8 \
  file://${SUBMODULE_BIGCODE}/LICENSE;md5=dc6bd4d967e085fe783aa2abe7655c60 \
"

SRCREV_onl ?= "${AUTOREV}"
SRCREV_infra ?= "168b695e51241be2823111f105b129236a1d79f8"
SRCREV_bigcode ?= "7294ff56e750c188d1f3b074ffbadd2024d50089"

URI_ONL ?= "git://github.com/dentproject/dentOS.git;branch=main;protocol=https"
URI_INFRA ?= "git://github.com/floodlight/infra.git"
URI_BIGCODE ?= "git://github.com/floodlight/bigcode.git"

SRCREV_FORMAT = "onl_infra_bigcode"
ONLP_VERSION = "dent"
# submodules are checked out individually to support license file checking
SRC_URI = "${URI_ONL};name=onl \
           ${URI_INFRA};name=infra;destsuffix=git/${SUBMODULE_INFRA} \
           ${URI_BIGCODE};name=bigcode;destsuffix=git/${SUBMODULE_BIGCODE} \
           file://onlpdump.service \
           file://baseconf.service \
           file://gcc-strncpy-fix.patch \
           file://ar.patch;patchdir=${SUBMODULE_INFRA} \
           file://0002-fix-Werror-unused-result.patch \
"

inherit systemd

#require dent.inc

SYSTEMD_SERVICE_${PN} = "onlpdump.service"
SYSTEMD_AUTO_ENABLE = "enable"

DEPENDS = "ifupdown2 i2c-tools python libedit libzip"

S = "${WORKDIR}/git"
PV = "1.1+git${SRCPV}"

PACKAGE_ARCH = "${MACHINE_ARCH}"
PROVIDES += "libonlp libonlp-platform libonlp-platform-defaults"
INSANE_SKIP_${PN} = "file-rdeps"

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

PYTHON_MAJMIN = "2.7"

EXTRA_OEMAKE = "\
  'AR=${AR}' \
  'ARCH=${ONL_ARCH}' \
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

SYSTEMD_SERVICE_${PN} = "\
    onlpdump.service \
    baseconf.service \
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
  V=1 VERBOSE=1 oe_runmake -C packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONL_MACHINE}/${ONL_MACHINE}/onlp/builds alltargets
}

do_install() {

    install -d \
    ${D}${bindir} \
    ${D}${includedir}/AIM \
    ${D}${includedir}/BigList \
    ${D}${includedir}/IOF \
    ${D}${includedir}/cjson \
    ${D}${includedir}/onlp \
    ${D}${includedir}/onlplib \
    ${D}${libdir} \

    # This is stupid and lazy and I'm working around ONLP stuff.

    install -d ${D}/lib/${ONIE_ARCH}-linux-gnu

    #install platform config and onlpdump
    # /lib/platform-config/
    # ├── current -> x86-64-accton-asgvolt64-r0
    # └── x86-64-accton-asgvolt64-r0
    #     └── onl
    #         ├── bin
    #         │   └── onlpdump
    #         ├── lib
    #         │   └── libonlp-x86-64-accton-asgvolt64.so 
    #         └── x86-64-accton-asgvolt64-r0.yml 
    
    # /lib first
    install -d \
        ${D}/lib/platform-config/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}/onl/bin \
        ${D}/lib/platform-config/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}/onl/lib

    # .so file
    install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONL_MACHINE}/${ONL_MACHINE}/onlp/builds/lib/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}.so ${D}/lib/platform-config/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}/onl/lib

    # platform yml file
    install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONL_MACHINE}/${ONL_MACHINE}/platform-config/r${ONIE_MACHINE_REV}/src/lib/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}.yml ${D}/lib/platform-config/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}/onl

    # this is a dangerous hack
    sed -i '/kernel-4-/d' ${D}/lib/platform-config/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}/onl/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}.yml

    # install onlpdump in bindir and link to lib/platformconf/*/onl/bin
    # Some code needs to go here to figure out if it's onlpdump or onlps. for now assume onlps.
    install -d ${D}${bindir} \
        ${D}${libdir} 

    install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONL_MACHINE}/${ONL_MACHINE}/onlp/builds/onlpdump/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/onlps ${D}${bindir}
    ln -r -s ${D}${bindir}/onlps ${D}/lib/platform-config/${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}/onl/bin/onlps

    # install onlpdump.py and libs
    install -d \ 
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/pki \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform/${ONL_VENDOR} \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform/${ONIE_ARCH}_${ONL_VENDOR}_${ONL_MACHINE}_r${ONIE_MACHINE_REV} \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlp \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlplib \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/sff \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/test \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/sysconfig \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/uboot \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/upgrade \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/util \
        ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/versions

    # install onlpdump.py
    install -m 0755 packages/base/any/onlp/src/onlpdump.py ${D}${bindir}
    # install onlp modules
    install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/__init__.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/
    install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/onlp/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlp/
    install -m 0755 packages/base/any/onlp/src/onlp/module/python/onlp/test/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/test/

    #install all the base python modules
    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/__init__.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl

    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/YamlUtils.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/bootconfig
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/bootconfig/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/bootconfig

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/versions
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/versions/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/versions

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/pki
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/pki/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/pki

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/platform/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/sysconfig
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/sysconfig/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/sysconfig

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/util
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/util/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/util

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/upgrade
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/upgrade/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/upgrade

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/network
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/network/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/network

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/grub
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/grub/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/grub

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/mounts
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/mounts/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/mounts


    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install
    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/plugins
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/plugins/__init__.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/plugins

    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/App.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/BaseInstall.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/BaseRecovery.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/ConfUtils.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/Fit.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/InstallUtils.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/Legacy.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/Plugin.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/RecoverApp.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/ShellApp.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/SystemInstall.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/install/__init__.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/install/

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/uboot
    install -m 755 packages/base/all/vendor-config-onl/src/python/onl/uboot/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/uboot
    
    # install platform cruft
    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform/${ONIE_VENDOR}/
    install -m 755 packages/platforms/${ONIE_VENDOR}/vendor-config/src/python/${ONIE_VENDOR}/__init__.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform/${ONIE_VENDOR}/

    install -d ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform/${ONIE_ARCH}_${ONL_VENDOR}_${ONIE_MACHINE}_r${ONIE_MACHINE_REV}/
    install -m 755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONL_MACHINE}/${ONL_MACHINE}/platform-config/r${ONIE_MACHINE_REV}/src/python/${ONIE_ARCH}_${ONL_VENDOR}_${ONIE_MACHINE}_r${ONIE_MACHINE_REV}/__init__.py ${D}/${libdir}/python${PYTHON_MAJMIN}/onl/platform/${ONIE_ARCH}_${ONIE_VENDOR}_${ONIE_MACHINE}_r${ONIE_MACHINE_REV}/

    install -m 0644 packages/base/any/onlp/src/onlp/module/inc/onlp/*.h ${D}${includedir}/onlp/

    # install onlplib modules
    install -m 0755 packages/base/any/onlp/src/onlplib/module/python/onlp/onlplib/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/onlplib/
    # install headers
    install -m 0644 packages/base/any/onlp/src/onlplib/module/inc/onlplib/*.h ${D}${includedir}/onlplib/

    install -m 0755 sm/bigcode/modules/sff/module/python/sff/* ${D}/${libdir}/python${PYTHON_MAJMIN}/onlp/sff
    # install headers
    install -m 0644 sm/bigcode/modules/BigData/BigList/module/inc/BigList/*.h ${D}${includedir}/BigList/
    install -m 0644 sm/bigcode/modules/IOF/module/inc/IOF/*.h ${D}${includedir}/IOF/
    install -m 0644 sm/bigcode/modules/cjson/module/inc/cjson/*.h ${D}${includedir}/cjson/
    install -m 0644 sm/infra/modules/AIM/module/inc/AIM/*.h ${D}${includedir}/AIM/

    # install libonlp-platform shared library (includes AIM.a  AIM_posix.a  BigList.a  cjson.a  cjson_util.a  IOF.a  onlplib.a  x86_64_delta_ag7648.a)
    #
    install -m 0755 packages/platforms/${ONIE_VENDOR}/${ONL_ARCH}/${ONL_MACHINE}/${ONL_MACHINE}/onlp/builds/lib/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}.so ${D}${libdir}
    mv ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}.so ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}.so.1
    ln -r -s ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}.so.1 ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}.so
    ln -r -s ${D}${libdir}/libonlp-${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}.so.1 ${D}${libdir}/libonlp-platform.so.1

    # install libonlp shared library (includes TODO)
    install -m 0755 packages/base/any/onlp/builds/onlp/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp.so ${D}${libdir}
    mv ${D}${libdir}/libonlp.so ${D}${libdir}/libonlp.so.1
    ln -r -s ${D}${libdir}/libonlp.so.1 ${D}${libdir}/libonlp.so

    # install libonlp shared library (includes TODO)
    install -m 0755 packages/base/any/onlp/builds/onlp-platform-defaults/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/libonlp-platform-defaults.so ${D}${libdir}
    mv ${D}${libdir}/libonlp-platform-defaults.so ${D}${libdir}/libonlp-platform-defaults.so.1
    ln -r -s ${D}${libdir}/libonlp-platform-defaults.so.1 ${D}${libdir}/libonlp-platform-defaults.so

    # install onlpd
    install -d ${D}/bin
    install -m 0755 packages/base/any/onlp/builds/onlpd/BUILD/${ONL_DEBIAN_SUITE}/${TOOLCHAIN}/bin/onlpd ${D}/bin 

    # onlpdump service file
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/onlpdump.service ${D}${systemd_unitdir}/system

    # onl platform conf 
    install -d ${D}${sysconfdir}/onl
    echo "${ONL_ARCH}-${ONL_VENDOR}-${ONL_MACHINE}-r${ONIE_MACHINE_REV}" > ${D}${sysconfdir}/onl/platform

    # onl-platform-baseconf
    install -d ${D}${sysconfdir}/baseconf
    install -m 0755 packages/base/all/vendor-config-onl/src/boot.d/51.onl-platform-baseconf ${D}${sysconfdir}/baseconf/onl-platform-baseconf.py
    install -m 0644 ${WORKDIR}/baseconf.service ${D}${systemd_unitdir}/system

    # fix up bindir and sysconfdir
    sed -i -e 's,@BINDIR@,/bin,g' \
         ${D}${systemd_unitdir}/system/*.service
    sed -i -e 's,@SYSCONFDIR@,${sysconfdir},g' \
         ${D}${systemd_unitdir}/system/*.service

}

FILES_${PN} = "${libdir}/python${PYTHON_MAJMIN} \ 
    ${sysconfdir} \
    ${sysconfdir}/baseconf \
    ${sysconfdir}/onl \
    ${systemd_unitdir}/system \
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
    /lib/${ONIE_ARCH}-linux-gnu \
    /bin/ \
"

