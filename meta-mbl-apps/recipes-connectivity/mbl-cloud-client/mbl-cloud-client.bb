# Copyright (c) 2018 Arm Limited and Contributors. All rights reserved.
#
# SPDX-License-Identifier: MIT

SUMMARY="Public mbed Cloud client for mbed Linux"
DESCRIPTION="Provides a mechanism to access ARM's mbed Cloud services from mbed Linux"
HOMEPAGE="https://github.com/ARMmbed/mbl-core/tree/master/cloud-services/mbl-cloud-client"

LICENSE="Apache-2.0"
LIC_FILES_CHKSUM = "file://${S}/LICENSE.Apache-2.0;md5=e3fc50a88d0a364313df4b21ef20c29e"
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

S = "${WORKDIR}/git"

# common sources for mbl-cloud-client(public) and mbl-cloud-client-internal
SRC_URI_COMMON = "file://yocto-toolchain.cmake \
  file://arm_update_local_config.sh \
  file://mbl-cloud-client.service \
  "

# specific sources for the mbl-cloud-client public version
SRC_URI_MBL_CLOUD_CLIENT_PUBLIC = "${SRC_URI_MBL_CORE_REPO} \
  file://arg_too_long_fix_1.patch;patchdir=${S} \
  file://arg_too_long_fix_2.patch;patchdir=${S}/cloud-services/mbl-cloud-client/mbed-cloud-client \
  file://linux-paths-update-client-pal-filesystem.patch;patchdir=${S}/cloud-services/mbl-cloud-client/mbed-cloud-client \
  file://check-min-pthread-stack-size.patch;patchdir=${S}/cloud-services/mbl-cloud-client/mbed-cloud-client \
  "

# all sources for the mbl-cloud-client public version
SRC_URI = "${SRC_URI_COMMON} ${SRC_URI_MBL_CLOUD_CLIENT_PUBLIC}"

SRCREV = "${SRCREV_MBL_CORE_REPO}"

DEPENDS = " glibc jsoncpp"

RDEPENDS_${PN} = "\
    e2fsprogs-mke2fs \
    libgcc \
    libstdc++ \
    util-linux \
    mbl-dbus-cloud \
"

# Installed packages
PACKAGES = "${PN}-dbg ${PN}"

FILES_${PN} += "\
    /opt \
    /opt/arm \
    /opt/arm/mbl-cloud-client \
    /opt/arm/arm_update_activate.sh \
    /opt/arm/arm_update_active_details.sh \
    /opt/arm/arm_update_cmdline.sh \
    /opt/arm/arm_update_common.sh \
    /opt/arm/arm_update_local_config.sh \
    ${sysconfdir}/logrotate.d/mbl-cloud-client-logrotate.conf \
"

FILES_${PN}-dbg += "/opt/arm/.debug \
                    /usr/src/debug/mbl-cloud-client"

# !!!
# Note: currently, we use x86_x64 PC Linux PAL platform implementation that is intented
# for test purposes. That means, that we have test-quality code with possible defects
# and other non-production code issues.
TARGET = "x86_x64_NativeLinux_mbedtls"

export SSH_AUTH_SOCK

# Allowed [Debug|Release]
RELEASE_TYPE="Debug"

inherit pythonnative
inherit cmake
inherit systemd

SYSTEMD_SERVICE_${PN} = "mbl-cloud-client.service"

do_setup_pal_env() {
    echo "Setup pal env"
    CUR_DIR=$(pwd)

    cd "${S}/cloud-services/mbl-cloud-client/"

    # Clean the old build directory
    rm -rf "__${TARGET}"
    mbed deploy --protocol ssh
    python ./pal-platform/pal-platform.py -v deploy --target="${TARGET}" generate
    cd ${CUR_DIR}
}

addtask setup_pal_env after do_unpack before do_patch
do_setup_pal_env[depends] += "mbed-cli-native:do_populate_sysroot"
do_setup_pal_env[depends] += "python-click-native:do_populate_sysroot"
do_setup_pal_env[depends] += "python-requests-native:do_populate_sysroot"
do_setup_pal_env[depends] += "python-urllib3-native:do_populate_sysroot"
do_setup_pal_env[depends] += "python-chardet-native:do_populate_sysroot"
do_setup_pal_env[depends] += "python-certifi-native:do_populate_sysroot"
do_setup_pal_env[depends] += "python-idna-native:do_populate_sysroot"

do_configure() {
    CUR_DIR=$(pwd)
    cd "${S}/cloud-services/mbl-cloud-client/__${TARGET}"
    cp "${WORKDIR}/yocto-toolchain.cmake" "${S}/cloud-services/mbl-cloud-client/pal-platform/Toolchain/GCC"

    cmake -G "Unix Makefiles" \
          -DCMAKE_BUILD_TYPE="${RELEASE_TYPE}" \
          -DCMAKE_TOOLCHAIN_FILE="${S}/cloud-services/mbl-cloud-client/pal-platform/Toolchain/GCC/yocto-toolchain.cmake" \
          -DEXTARNAL_DEFINE_FILE="${S}/cloud-services/mbl-cloud-client/define.txt"

    cd ${CUR_DIR}
}

do_compile() {
    CUR_DIR=$(pwd)
    cd "${S}/cloud-services/mbl-cloud-client/__${TARGET}"
    oe_runmake mbl-cloud-client
    oe_runmake pelion-provisioning-util
    cd ${CUR_DIR}
}

do_install() {
    install -d "${D}/opt/arm"
    install "${S}/cloud-services/mbl-cloud-client/__${TARGET}/${RELEASE_TYPE}/mbl-cloud-client" "${D}/opt/arm"
    install "${S}/cloud-services/mbl-cloud-client/__${TARGET}/${RELEASE_TYPE}/pelion-provisioning-util" "${D}/opt/arm"

    install -m 755 "${S}/cloud-services/mbl-cloud-client/scripts/arm_update_activate.sh" "${D}/opt/arm"
    install -m 755 "${S}/cloud-services/mbl-cloud-client/scripts/arm_update_active_details.sh" "${D}/opt/arm"
    install -m 755 "${S}/cloud-services/mbl-cloud-client/scripts/arm_update_common.sh" "${D}/opt/arm"
    install -m 755 "${S}/cloud-services/mbl-cloud-client/mbed-cloud-client/update-client-hub/modules/pal-linux/scripts/arm_update_cmdline.sh" "${D}/opt/arm"
    install -m 755 "${WORKDIR}/arm_update_local_config.sh" "${D}/opt/arm"

    install -d "${D}${systemd_unitdir}/system/"
    install -m 0644 "${WORKDIR}/mbl-cloud-client.service" "${D}${systemd_unitdir}/system/"
}

# Add a logrotate config files
MBL_LOGROTATE_CONFIG_LOG_NAMES = "mbl-cloud-client arm_update_active_details arm_update_activate"

MBL_LOGROTATE_CONFIG_LOG_PATH[mbl-cloud-client] = "/var/log/mbl-cloud-client.log"
MBL_LOGROTATE_CONFIG_SIZE[mbl-cloud-client] ?= "1M"
MBL_LOGROTATE_CONFIG_ROTATE[mbl-cloud-client] ?= "4"
MBL_LOGROTATE_CONFIG_POSTROTATE[mbl-cloud-client] = "/usr/bin/killall -HUP mbl-cloud-client"

MBL_LOGROTATE_CONFIG_LOG_PATH[arm_update_active_details] = "/var/log/arm_update_active_details.log"
MBL_LOGROTATE_CONFIG_SIZE[arm_update_active_details] ?= "128k"
MBL_LOGROTATE_CONFIG_ROTATE[arm_update_active_details] ?= "4"

MBL_LOGROTATE_CONFIG_LOG_PATH[arm_update_activate] = "/var/log/arm_update_activate.log"
MBL_LOGROTATE_CONFIG_SIZE[arm_update_activate] ?= "128k"
MBL_LOGROTATE_CONFIG_ROTATE[arm_update_activate] ?= "4"

inherit mbl-logrotate-config