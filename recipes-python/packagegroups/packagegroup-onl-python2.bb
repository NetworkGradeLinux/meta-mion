SUMMARY = "Just what we need for ONL"

inherit packagegroup

PROVIDES = "${PACKAGES}"
PACKAGES = ' \
    packagegroup-onl-python2 \
'

RDEPENDS_packagegroup-onl-python2 = "\
    python python-attrs python-attr python-configparser python-configargparse \ 
"

EXCLUDE_FROM_WORLD = "1"
