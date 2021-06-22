FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
SUMMARY = "Stratum binary install from provided .deb files"
LICENSE = "CLOSED"

SRC_URI = "https://github.com/stratum/stratum/releases/download/2021-03-31/stratum-bfrt-21.03-9.4.0-amd64.deb \
file://stratum.conf"

SRC_URI[sha256sum] = "1901a13c8c68d990bd7392a8adf5626fef61b91f71f569fe5163c579ab352eb9"

do_compile[noexec] = "1"
do_configure[nostamp] += "1"

INSANE_SKIP_${PN} = "already-stripped file-rdeps libdir dev-so"
INSANE_SKIP_${PN}-dev += "dev-elf dev-so"

RDEPENDS_${PN} = " \
    barefoot-bsp \
    bf-drivers \
    bf-kdrv \
    bf-syslibs \
    bf-utils \
    boost \
    boost-thread \
    bzip2 \
    grpc \
    judy \
    kmod \
    libedit \
    openssl \
    pi \
    procps \
    python3 \
    systemd \
"

do_configure () {
    [ -d ${S} ] || mkdir -p ${S}
    cd ${S}
    ar x ${DL_DIR}/stratum-bfrt-21.03-9.4.0-amd64.deb
    tar xf data.tar.bz2
}

do_install () {
    install -d ${D}${bindir}
    cp -r ${S}/usr/bin/* ${D}${bindir}

    install -d ${D}${sysconfdir}/stratum/
    cp -r ${S}/etc/stratum/* ${D}${sysconfdir}/stratum/

    install -d ${D}${libdir}/systemd/system/
    cp -r ${S}/usr/lib/systemd/system/* ${D}${libdir}/systemd/system/

    install -d ${D}/opt/usr/lib/
    cp -r ${S}/usr/lib/* ${D}/opt/usr/lib/

    install -d ${D}${datadir}/tofino_sds_fw/
    cp -r ${S}/usr/share/tofino_sds_fw ${D}${datadir}/tofino_sds_fw/
    install -d ${D}${datadir}/stratum
    cp -r ${S}/usr/share/stratum ${D}${datadir}

    install -d ${D}/lib64
    cd ${D}/lib64
    ln -s ../lib/ld-linux-x86-64.so.2 ld-linux-x86-64.so.2
    install -d ${D}/etc/ld.so.conf.d/
    install -m 0755 ${WORKDIR}/stratum.conf ${D}/etc/ld.so.conf.d/
}

FILES_${PN} += " \
    ${bindir} \
    ${sysconfdir}/stratum \
    ${sysconfdir}/ld.so.conf.d/stratum.conf \
    ${libdir}/systemd/system \
    ${datadir}/tofino_sds_fw \
    ${datadir}/stratum \
    /opt \
    /lib64 \
" 
