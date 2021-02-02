#!/bin/sh
#  Copyright (C) 2014-2015 Curt Brune <curt@cumulusnetworks.com>
#  Copyright (C) 2014,2015,2016 david_yang <david_yang@accton.com>
#  Copyright (C) 2016 Pankaj Bansal <pankajbansal3073@gmail.com>
#  Copyright (C) 2020 APS Networks FIXME
#
#  SPDX-License-Identifier:     GPL-2.0
#
#  ONIE install script for Mion OS

set -e
#set -x

###############################################################################

VOLUME_LABEL="MION-OS"
ROOTFS_FILE="rootfs.tar.xz"
KERNEL_FILE="bzImage"
MION_VERSION=
GRUB_ENTRY="Mion OS ${MION_VERSION}"
ROOTFS_TYPE="ext4"
ROOTFS_SIZE_MB=

ONIE_BOOT_UUID="C12A7328-F81F-11D2-BA4B-00A0C93EC93B"

###############################################################################

# Logging
info(){ echo "INFO: $1"; }
warn(){ echo "WARNING: $1"; }
error(){ echo "ERROR: $1"; exit 1; }
#FIXME do we want to keep the install log so we can see it after the install?

# Creates a new GPT partition for the OS.
#
# arg $1 -- base block device
#
# Returns the created partition number in $part_num
create_gpt_partition()
{
    blk_dev="$1"

    # Check if OS partition already exists
    part_num=$(sgdisk --print "${blk_dev}" | grep ${VOLUME_LABEL} | awk '{print $1}')
    if [ -n "${part_num}" ] ; then # Delete existing partition
        sgdisk -d "${part_num}" "${blk_dev}" || \
            error "Unable to delete partition ${part_num} on ${blk_dev}"
        partprobe
    fi

    # Find next available partition
    part_num=$(sgdisk --print "${blk_dev}" | awk 'END{print $1 += 1}')

    blk_suffix=
    echo "${blk_dev}" | grep -q 'mmcblk\|nvme' && blk_suffix="p"

    # Create new partition
    info "Creating new GPT partition ${blk_dev}${blk_suffix}${part_num}"

    PART_SIZE=0 # Use the max space available by default
    [ ! -z "${ROOTFS_SIZE_MB}" ] && PART_SIZE="+${ROOTFS_SIZE_MB}MB"

    gpt_attr_bitmask="0x0" #FIXME is this needed or is there a default?
    sgdisk --new="${part_num}:0:${PART_SIZE}" \
        --attributes="${part_num}:=:${gpt_attr_bitmask}" \
        --change-name="${part_num}:${VOLUME_LABEL}" "${blk_dev}" ||
            error "Unable to create partition ${part_num} on ${blk_dev}"
    partprobe
}

# Creates a new MSDOS partition for the OS.
#
# arg $1 -- base block device
#
# Returns the created partition number in $part_num
create_msdos_partition()
{
    blk_dev="$1"

    # Check if OS partition already exists -- filesystem label.
    part_info="$(blkid | grep ${VOLUME_LABEL} | awk -F: '{print $1}')"
    if [ -n "${part_info}" ]; then
        # Delete existing partition
        part_num="$(echo -n "${part_info}" | sed -e "s#${blk_dev}##")"
        parted -s "${blk_dev}" rm "${part_num}" || \
            error "Unable to delete partition ${part_num} on ${blk_dev}"
        partprobe
    fi

    # Find next available partition
    last_part_info="$(parted -s -m "${blk_dev}" unit s print | tail -n 1)"
    last_part_num="$(echo -n "${last_part_info}" | awk -F: '{print $1}')"
    last_part_end="$(echo -n "${last_part_info}" | awk -F: '{print $3}')"
    # Remove trailing 's'
    last_part_end=${last_part_end%s}
    part_num=$((last_part_num + 1))
    part_start=$((last_part_end + 1))
    # sectors_per_mb = (1024 * 1024) / 512 = 2048
    sectors_per_mb=2048
    part_end=$((part_start + (PART_SIZE_MB * sectors_per_mb) - 1))

    # Create new partition
    info "Creating new MSDOS partition ${blk_dev}${blk_suffix}${part_num}"
    parted -s --align optimal "${blk_dev}" unit s \
      mkpart primary ${part_start} ${part_end} set ${part_num} boot on || \
        error "Problems creating MSDOS partition ${part_num} on ${blk_dev}"
    partprobe
}

# For UEFI systems, create a new partition for the OS.
#
# arg $1 -- base block device
#
# Returns the created partition number in $part_num
create_uefi_partition()
{
    create_gpt_partition "$1"

    # Erase any related EFI BootOrder variables from NVRAM.
    for b in $(efibootmgr | grep "${VOLUME_LABEL}" | awk '{ print $1 }'); do
        local num=${b#Boot}
        num=${num%\*} #Remove trailing '*'
        efibootmgr -b "${num}" -B > /dev/null 2>&1
    done
}

# Install legacy BIOS GRUB
install_grub()
{
    local mnt="$1" blk_dev="$2"

    # Get running machine from conf file
    [ -r /etc/machine.conf ] && . /etc/machine.conf

    if [ "${onie_firmware}" = "coreboot" ]; then
        local grub_target="i386-coreboot"
    else
        local grub_target="i386-pc"
    fi

    # Install GRUB into the MBR of $blk_dev.
    grub-install --target=${grub_target} --boot-directory="${mnt}" --recheck "${blk_dev}" || \
        error "grub-install failed on: ${blk_dev}"
}

# Install UEFI GRUB
install_uefi_grub()
{
    local mnt="$1" blk_dev="$2"

    # Get running machine from conf file
    [ -r /etc/machine.conf ] && . /etc/machine.conf

    # Check if the EFI system partition UUID is on the same block device as the ONIE-BOOT partition
    local uefi_part=0
    for p in $(seq 8); do
        if sgdisk -i "$p" "${blk_dev}" | grep -q ${ONIE_BOOT_UUID}; then
            uefi_part=$p
            break
        fi
    done

    [ "${uefi_part}" -eq 0 ] && error "Unable to determine UEFI system partition"

    if [ "${onie_secure_boot}" = "yes" ]; then
        # ONIE is booting via shim, so the OS needs to also
        local loader_dir="/boot/efi/EFI/${VOLUME_LABEL}"
        mkdir -p ${loader_dir} || error "Unable to create directory ${loader_dir}"

        # Use ONIE's .efi binaries
        cp -a "/boot/efi/EFI/onie/"*"${onie_uefi_arch}.efi" ${loader_dir} || \
            error "Unable to copy ONIE .efi binaries to ${loader_dir}"

        local boot_uuid
        boot_uuid=$(grub-probe --target=fs_uuid "${mnt}") || \
            error "Unable to determine UUID of GRUB boot directory ${mnt}"

        # Generate tiny grub config for monolithic image
        cat <<EOF > "${loader_dir}/grub.cfg"
search.fs_uuid ${boot_uuid} root
echo "Search for uuid ${boot_uuid}"
echo "Found root: \$root"
set prefix=(\$root)'/grub'
configfile \$prefix/grub.cfg
EOF

        # Install primary grub config in $mnt
        grub_dir="${mnt}/grub"
        mkdir -p "${grub_dir}/fonts" "${grub_dir}/locale"

    else # No secure boot

        grub_install_log=$(mktemp)
        grub-install --no-nvram --bootloader-id=${VOLUME_LABEL} \
            --efi-directory="/boot/efi" --boot-directory="${mnt}" \
            --recheck "${blk_dev}" > "/${grub_install_log}" 2>&1 || {
                cat "${grub_install_log}" && rm -f "${grub_install_log}"
                error "grub-install failed on ${blk_dev}"
            }
        rm -f "${grub_install_log}"
    fi

    # Configure EFI NVRAM Boot variables, --create also sets the new boot number as active
    efibootmgr --quiet --create --label ${VOLUME_LABEL} \
        --disk "${blk_dev}" --part "${uefi_part}" \
        --loader "/EFI/${VOLUME_LABEL}/${onie_uefi_boot_loader}" || \
            error "efibootmgr failed to create new boot variable on ${blk_dev}"
}

###############################################################################

cd "$(dirname "$0")"

# Get the platform name
. ./machine.conf

. /lib/onie/onie-blkdev-common

info "Mion Installer -- ${PLATFORM}"

# Install OS on same block device as ONIE
blk_dev=$(blkid | grep ONIE-BOOT | awk '{print $1}' | sed -e 's/[1-9][0-9]*:.*$//' |\
    sed -e 's/\([0-9]\)\(p\)/\1/' | head -n 1)

[ -b "${blk_dev}" ] || error "Unable to determine block device of ONIE install"

# Auto-detect whether BIOS or UEFI
[ -d "/sys/firmware/efi/efivars" ] && firmware="uefi" || firmware="bios"

# Determine ONIE partition type
onie_partition_type=$(onie-sysinfo -t)

if [ ${firmware} = "uefi" ]; then
    create_partition="create_uefi_partition"
elif [ "${onie_partition_type}" = "gpt" ]; then
    create_partition="create_gpt_partition"
elif [ "${onie_partition_type}" = "msdos" ]; then
    create_partition="create_msdos_partition"
else
    error "Unsupported partition type ${onie_partition_type}"
fi

eval ${create_partition} "${blk_dev}"

# Get the correct device name for mmcblk/nvme
dev=$(echo "${blk_dev}" | sed -e 's/\(mmcblk[0-9]\)/\1p/')${part_num}
echo "${blk_dev}" | grep -q nvme && \
    dev=$(echo "${blk_dev}" | sed -e 's/\(nvme[0-9]n[0-9]\)/\1p/')${part_num}

partprobe

# Create a filesystem on the partition with a label
mkfs.${ROOTFS_TYPE} -F -L ${VOLUME_LABEL} "${dev}" || error "Unable to create file system on ${dev}"

# Mount the filesystem on a temp dir
mnt=$(mktemp -d) || error "Unable to create file system mount point"

mount -t ${ROOTFS_TYPE} -o defaults,rw "${dev}" "${mnt}" || error "Unable to mount ${dev} on ${mnt}"

# Install the rootfs and kernel on the new partition
xzcat rootfs.tar.xz | tar xf - -C "${mnt}" || error "Failed to install rootfs"

# store installation log in the file system
onie-support "${mnt}"

# Set a few GRUB_xxx environment variables that will be picked up and
# used by the 50_onie_grub script.  This is similiar to what an OS
# would specify in /etc/default/grub.
#
# GRUB_SERIAL_COMMAND
# GRUB_CMDLINE_LINUX

[ -r "./platform.conf" ] && . "./platform.conf"

# Import console config and linux cmdline
[ -r "${onie_root_dir}/grub/grub-variables" ] && . "${onie_root_dir}/grub/grub-variables"

# Install GRUB for the NOS.
if [ ${firmware} = "uefi" ]; then
    install_uefi_grub "${mnt}" "${blk_dev}"
else
    install_grub "${mnt}" "${blk_dev}"
fi

# Create a minimal grub.cfg that allows for:
#   - configure the serial console
#   - allows for grub-reboot to work
#   - a menu entry for the OS
#   - menu entries for ONIE
grub_cfg=$(mktemp)

#part=$(sgdisk -p "${blk_dev}" | grep "${VOLUME_LABEL}" | awk '{print $1}')
#part_uuid="$(sgdisk -i "${part}" "${blk_dev}" | grep 'Partition unique GUID' | cut -d\  -f 4)"

# Add common configuration, like the timeout and serial console.
cat <<EOF > "${grub_cfg}"
${GRUB_SERIAL_COMMAND}
terminal_input ${GRUB_TERMINAL_INPUT}
terminal_output ${GRUB_TERMINAL_OUTPUT}

set timeout=5

EOF

# Add the logic to support grub-reboot and a menu entry for the OS
cat <<EOF >> "${grub_cfg}"
if [ -s \$prefix/grubenv ]; then
  load_env
fi
if [ "\${next_entry}" ] ; then
   set default="\${next_entry}"
   set next_entry=
   save_env next_entry
fi
EOF

# Add a menu entry for Mion
cat <<EOF >> "${grub_cfg}"
onie_partition_type=${onie_partition_type}
export onie_partition_type

function entry_start {
  insmod gzio
  insmod ext2
  if [ "\$onie_partition_type" = "gpt" ] ; then
    insmod part_gpt
    set root='(hd0,gpt${part_num})'
  else
    insmod part_msdos
    set root='(hd0,msdos${part_num})'
  fi
}

menuentry '${GRUB_ENTRY}' {
        search --no-floppy --label --set=root ${VOLUME_LABEL}
        echo    'Loading ${GRUB_ENTRY} ...'
        linux   /boot/bzImage ${GRUB_CMDLINE_LINUX} rootfstype=${ROOTFS_TYPE} root=PARTLABEL=${VOLUME_LABEL} rootwait ${EXTRA_CMDLINE_LINUX}
}

EOF

# Add menu entries for ONIE, use the grub fragment provided by the ONIE distribution.
"${onie_root_dir}/grub.d/50_onie_grub" >> "${grub_cfg}"

cp "${grub_cfg}" "${mnt}/grub/grub.cfg"

# Clean up
umount "${mnt}" || warn "Problems umounting ${mnt}"
cd /

# Set NOS mode if available
if [ -x /bin/onie-nos-mode ]; then
    /bin/onie-nos-mode -s
fi
