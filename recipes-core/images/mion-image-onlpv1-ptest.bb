# SPDX-License-Identifier: MIT

# Currently requires ptest to be set in local.conf as a member of
# DISTRO_FEATURES, as is proper. However, we need to eventually have that setup
# and working here.

require mion-image-onlpv1.bb

#DISTRO_FEATURES_append += " ptest"

IMAGE_FEATURES += " ptest-pkgs"
IMAGE_INSTALL += "ptest-runner"
