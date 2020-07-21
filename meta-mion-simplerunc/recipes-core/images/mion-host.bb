SUMMARY = "mion host image recipe."
LICENSE = "MIT"

require mion-host-core.inc

IMAGE_INSTALL += " \
    ${CONTAINER_DEPENDS} \
"


