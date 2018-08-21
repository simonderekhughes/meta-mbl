DESCRIPTION = "ARM Trusted Firmware Warp7"

DEPENDS += " coreutils-native optee-os u-boot linux-fslc u-boot-mkimage-native atf-fiptool-native"

SRC_URI = "git://github.com/ARM-software/arm-trusted-firmware.git;protocol=https;nobranch=1;name=atf"
SRCREV = "36044baf08a9f816a8a062a8a50ede12a816a6dd"
SRC_URI +="file://u-boot.cfgout.warp7;name=uboot.cfgout;"
SRCREV_uboot.cfgout="6bb815da1bc986dc717a59cc6d2552f8"

# Notes on uboot.cfgout
# This is a file automatically generated by u-boot when compiling up a warp7
# image. uboot.cfgout is a necessary input when generating a .imx image
# To regenerate uboot.cfgout just do
# "make warp7_config;make u-boot.imx CROSS_COMPILE=your-x-compiler-"

LICENSE = "BSD-3-Clause & GPLv2"
LICENSE-atf = "BSD-3-Clause"
LICENSE-uboot.cfgout = "GPLv2"
LIC_FILES_CHKSUM = "file://license.rst;md5=e927e02bca647e14efd87e9e914b2443 \
                    file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

PLATFORM_imx7s-warp = "warp7"

# LDFLAGS is configured in bitbake.conf as linker flags to be passed to CC.
# It sets it to include "-Wl,-O1". The ATF build system inherits LDFLAGS
# from the environment and passes it directly to LD when building BL1,
# in conflict with the bitbake view. This then causes an error message
# in the ATF trace (aarch64-linux-gnu-ld.bfd: unrecognized option '-Wl,-O1').
# This problem is avoided by clearing LDFLAGS.
LDFLAGS[unexport] = "1"

do_compile() {
   oe_runmake -C ${S} BUILD_BASE=${B} \
      BUILD_PLAT=${B}/${PLATFORM} \
      PLAT=${PLATFORM} \
      ARCH=aarch32 \
      ARM_ARCH_MAJOR=7 \
      ARM_CORTEX_A7=yes \
      CROSS_COMPILE=${TARGET_PREFIX} \
      LOG_LEVEL=40 \
      AARCH32_SP=optee \
      all

    # Get the entry point
    ENTRY=`${HOST_PREFIX}readelf ${B}/${PLATFORM}/bl2/bl2.elf -h | grep "Entry" | awk '{print $4}'`

    # Generate the .imx binary
    uboot-mkimage -n ${WORKDIR}/u-boot.cfgout.warp7 -T imximage -e ${ENTRY} -d ${B}/${PLATFORM}/bl2.bin ${B}/${PLATFORM}/bl2.bin.imx

    # Copy required FIP collateral to a staging point
    install ${B}/${PLATFORM}/bl2.bin.imx ${DEPLOY_DIR_IMAGE}

    # Generate the FIP image
    atf-fiptool create \
        --tos-fw ${DEPLOY_DIR_IMAGE}/optee/tee-header_v2.bin \
        --tos-fw-extra1 ${DEPLOY_DIR_IMAGE}/optee/tee-pager_v2.bin \
        --tos-fw-extra2 ${DEPLOY_DIR_IMAGE}/optee/tee-pageable_v2.bin \
        --nt-fw ${DEPLOY_DIR_IMAGE}/u-boot.bin \
        --hw-config ${DEPLOY_DIR_IMAGE}/fiptemp/imx7s-warp.dtb \
        ${DEPLOY_DIR_IMAGE}/warp7.fip

    # Unify FIP and ATF into one binary
    FIP_SIZE=$(stat -c %s ${DEPLOY_DIR_IMAGE}/warp7.fip)
    dd if=/dev/zero of=${DEPLOY_DIR_IMAGE}/atf-bl2-fip.bin count=$(expr 32 \* 1024 \+ ${FIP_SIZE}) bs=1
    dd if=${DEPLOY_DIR_IMAGE}/bl2.bin.imx of=${DEPLOY_DIR_IMAGE}/atf-bl2-fip.bin
    # the packed image is burned to 1KB offset, so 1MB is shift to 1023KB in image
    dd if=${DEPLOY_DIR_IMAGE}/warp7.fip of=${DEPLOY_DIR_IMAGE}/atf-bl2-fip.bin bs=1024 seek=1023
}

do_clean_append() {
        bl2 = "%s/%s" % (d.expand("${DEPLOY_DIR_IMAGE}"), "bl2.bin.imx")
        fip = "%s/%s" % (d.expand("${DEPLOY_DIR_IMAGE}"), "warp7.fip")
        unified = "%s/%s" % (d.expand("${DEPLOY_DIR_IMAGE}"), "atf-bl2-fip.bin")

        oe.path.remove(bl2)
        oe.path.remove(fip)
        oe.path.remove(unified)
}