ONIE_VENDOR_SHORT ?= "${ONIE_VENDOR}"
ONIE_MACHINE_TYPE ?= "unknown"
ONIE_ARCH ?= "${TARGET_ARCH}"
ONIE_MACHINE ?= "${ONIE_VENDOR_SHORT}_${ONIE_MACHINE_TYPE}"
ONIE_MACHINE_REV ?= "0"
ONIE_PLATFORM ?= "${ONIE_ARCH}-${ONIE_MACHINE}-r${ONIE_MACHINE_REV}"

# onl vars
ONL_DEBIAN_SUITE = "yocto"
ONL_PLATFORM="${@'${ONIE_ARCH}-${ONIE_MACHINE}'.replace('_', '-')}"
ONL_ARCH="${@'${ONIE_ARCH}'.replace('_', '-')}"
