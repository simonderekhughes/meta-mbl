# Copyright (c) 2018 Arm Limited and Contributors. All rights reserved.
#
# SPDX-License-Identifier: MIT
inherit mbl-artifact-names

DEPENDS += "u-boot-tools-native"

require recipes-bsp/atf/atf.inc

PLATFORM = "imx8mm"

# SPD= tells ATF to use optee for runtime SPD instead of internal ATF code.
# CROSS_COMPILE points to the necessary cross compiler, something required due to the
#               older codebase we are working with for i.MX8
# NEED_BL32 and NEED_BL33 instruct the build to provision for BL32 and BL33 respectively
# BUILD_BL2 is the flag we use to optionally switch on BL2 building in the incoming
#           NXP ATF codebase
# fip instructs the makefile to generate a FIP
ATF_COMPILE_FLAGS += " SPD=opteed \
                       CROSS_COMPILE=aarch64-oe-linux- \
                       NEED_BL32=yes \
                       NEED_BL33=yes \
                       BUILD_BL2=1 \
                       fip "

# This is the name the imx boot tools expects "bl2-imx8mm.bin-optee"
ATF_NAME_APPEND = "${@bb.utils.contains('COMBINED_FEATURES', 'optee', '-optee', '', d)}"
MBL_UNIFIED_BIN = "bl2-${MACHINE}.bin${ATF_NAME_APPEND}"

do_compile_append() {
	# Create signed FIP image.
	oe_runmake ${ATF_COMPILE_FLAGS} fip
	cp ${B}/${PLATFORM}/bl2.bin ${B}/${PLATFORM}/${MBL_UNIFIED_BIN}
}

# We need to over-ride the default do_deploy with a NOP here.
# When we switch on FIP images signing we will need to do_deploy_append
do_deploy_append() {
	install -d ${DEPLOYDIR}/imx-boot-tools
	install -D -p -m 0644 ${B}/${PLATFORM}/${MBL_UNIFIED_BIN} ${DEPLOYDIR}/imx-boot-tools
	install -D -p -m 0644 ${B}/${PLATFORM}/${FIP_BIN} ${DEPLOYDIR}
}
