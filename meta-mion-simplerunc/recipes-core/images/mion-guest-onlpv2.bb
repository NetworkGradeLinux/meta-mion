SUMMARY = "Gaia ONLPv2 guest image recipe."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"
require mion-guest.inc

IMAGE_INSTALL += " \ 
 onlpv2 \
"
