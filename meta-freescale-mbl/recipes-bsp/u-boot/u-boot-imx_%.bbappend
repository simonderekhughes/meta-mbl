# Copyright (c) 2019 Arm Limited and Contributors. All rights reserved.
#
# SPDX-License-Identifier: MIT

inherit mbl-uboot-sign

SRCBRANCH = "imx_v2018.03_4.14.78_1.0.0_ga-mbl"
SRC_URI = "git://source.codeaurora.org/external/imx/uboot-imx.git;protocol=https;nobranch=1 \
    file://0001-imx8mm_evk_mbl-Add-an-mbed-Linux-imx8mm_evk-defconfi.patch \
    file://0002-imx8mm_evk_mbl-Add-an-mbed-Linux-imx8mm_evk-defconfi.patch \
    file://0003-arm-imx-Add-mbl-specific-boot-option.patch \
    file://0004-cmd-image_info-Add-checking-of-default-FIT-config.patch \
    file://0005-enable-fec-without-net-cmd.patch \
"

SRC_URI_append_imx8mmevk = " \
    ${@bb.utils.contains_any('PACKAGECONFIG','noconsole silent',' file://0003-set-silent-envs.patch file://0006-silent-spl-boot.patch','',d)} \
    ${@bb.utils.contains('PACKAGECONFIG','minimal',' file://0002-disable-dm-eth-mbl.cfg','',d)} \
    "

# MBL_UBOOT_VERSION should be updated to match version pointed to by SRCREV
MBL_UBOOT_VERSION = "2018.03"

SRCREV = "7ade5b407fe6164c0d07f32f72e487ae5f6f3964"

FILESEXTRAPATHS_prepend := "${THISDIR}/u-boot-imx:"


do_configure_prepend_imx8mmevk-mbl() {
    # change default boot partition
    sed -i 's/[#]define[[:space:]]*CONFIG_SYS_MMC_IMG_LOAD_PART[[:space:]]*.*/#define CONFIG_SYS_MMC_IMG_LOAD_PART ${UBOOT_DEFAULT_BOOT_PARTITION}/' ${S}/include/configs/imx8mq_evk.h
    
    # When setting UBOOT_CONFIG variable the do_configure defined in
    # u-boot.inc in oe-core doesn't call the merge_config.sh.
    # We added this workaround to call the merge_config.sh and prevent
    # patching the machine defconfig file for imx8mmevk-mbl.
    if [ -n "${UBOOT_CONFIG}" ]; then
        if [ -n "${UBOOT_MACHINE}" ]; then
            UBOOT_DEFCONFIG=$(echo "${UBOOT_MACHINE}" | sed 's/_config/_defconfig/g' | sed 's/ //g')
            KCONFIG_CONFIG=${S}/configs/${UBOOT_DEFCONFIG} merge_config.sh -m ${S}/configs/${UBOOT_DEFCONFIG} ${@" ".join(find_cfgs(d))}
        fi
    fi
}

do_compile_append_imx8mmevk-mbl() {
	# Copy device tree to default name for fit image signature verification usage.
	cp ${config}/dts/dt.dtb ${B}/${UBOOT_DTB_BINARY}
}

do_deploy_append() {
    # Fixup the name expected by the incoming imx-boot recipe
    cd ${DEPLOYDIR}/${BOOT_TOOLS}
    install -m 0777 ${B}/${config}/u-boot-nodtb.bin  ${DEPLOYDIR}/${BOOT_TOOLS}/u-boot-nodtb.bin
}

RM_WORK_EXCLUDE_ITEMS += "recipe-sysroot-native"
