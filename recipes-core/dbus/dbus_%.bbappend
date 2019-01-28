# Based on: openembedded-core/meta/recipes-core/dbus/dbus_1.12.10.bb
#           openembedded-core/meta/recipes-core/dbus/dbus/dbus-1.init
# In open-source project: http://git.openembedded.org/openembedded-core
#
# Original file: No copyright notice was included
# Modifications: Copyright (c) 2019 Arm Limited and Contributors. All rights reserved.
#
# SPDX-License-Identifier: MIT

# Make sure the local appending config file will be chosen by prepending an extra local path
FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

# Add extended functionality - mbl-cloud message bus 
PACKAGES =+ "${PN}-mbl-cloud"

# mbl-cloud.conf is not generated by the build system - it is maintained manually
SRC_URI =+ "file://dbus-mbl-cloud.init \
            file://mbl-cloud.conf \
"

# Declare another initscript package to start the dbus-daemon for the mbl-cloud bus by init manager
# Init script params are exactly as the systemwide bus - start as early as possible, on user/group ${DBUS_DAEMONUSER}
INITSCRIPT_PACKAGES += "${PN} ${PN}-mbl-cloud"
INITSCRIPT_NAME_${PN}-mbl-cloud = "dbus-mbl-cloud"
INITSCRIPT_PARAMS_${PN}-mbl-cloud = "start 03 5 3 2 . stop 20 0 1 6 ."


USERADD_PACKAGES =+ "${PN}-mbl-cloud"
GROUPADD_PARAM_${PN}-mbl-cloud = "-r netdev"
USERADD_PARAM_${PN}-mbl-cloud = "--system --home ${localstatedir}/lib/dbus \
                                --no-create-home --shell /bin/false \
                                --user-group ${DBUS_DAEMONUSER}"

# Declare configuration file to keep unchanged by installer
CONFFILES_${PN}-mbl-cloud = "${datadir}/dbus-1/mbl-cloud.conf"


# package depends on dbus, and extend its functionality
RDEPENDS_${PN}-mbl-cloud = "dbus"
RRECOMMENDS_dbus = "${PN}-mbl-cloud"

# dbus-mbl-cloud as the init file after rename, and mbl-cloud.conf is the configuration file
# The link to the init file is automatically crearted by the class update-rc.d 
FILES_${PN}-mbl-cloud = "${datadir}/dbus-1/mbl-cloud.d \
                        ${datadir}/dbus-1/mbl-cloud.conf \
                        ${nonarch_libdir}/systemd/mbl-cloud \
                        ${sysconfdir}/init.d/dbus-mbl-cloud \
"

# Create post install script for the installer
pkg_postinst_${PN}-mbl-cloud () {
    # If both systemd and sysvinit are enabled, mask the dbus-mbl-cloud init script
    if ${@bb.utils.contains('DISTRO_FEATURES','systemd sysvinit','true','false',d)}; then
        if [ -n "$D" ]; then
            OPTS="--root=$D"
        fi
        systemctl $OPTS mask dbus-mbl-cloud.service
    fi
}

# Install init and and configuration file on the image
do_install_append() {
    install -d ${D}${datadir}/dbus-1/mbl-cloud.d
    install -d ${D}${nonarch_libdir}/systemd/mbl-cloud    
    install -m 0644 ${WORKDIR}/mbl-cloud.conf ${D}${datadir}/dbus-1/mbl-cloud.conf
        
    if ${@bb.utils.contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
        install -d ${D}${sysconfdir}/init.d
        sed 's:@bindir@:${bindir}:' < ${WORKDIR}/dbus-mbl-cloud.init >${WORKDIR}/dbus-mbl-cloud.init.1
        sed 's:@datadir@:${datadir}:' < ${WORKDIR}/dbus-mbl-cloud.init.1 >${WORKDIR}/dbus-mbl-cloud.init.sh
        install -m 0755 ${WORKDIR}/dbus-mbl-cloud.init.sh ${D}${sysconfdir}/init.d/dbus-mbl-cloud
    fi
}

# The next parameters must match in both dbus-mbl-cloud file and mbl-cloud.conf.
# Use bbclass mbl-var-placeholders to replace all occurences in both files.
DBUS_DAEMONUSER="messagebus"
DBUS_PIDFILE="/var/run/dbus/pid_mbl-cloud-bus"
DBUS_MBL_CLOUD_BUS_ADDRESS="unix:path=/var/run/dbus/mbl_cloud_bus_socket"
MBL_VAR_PLACEHOLDER_FILES = "${D}${datadir}/dbus-1/mbl-cloud.conf ${D}${sysconfdir}/init.d/dbus-mbl-cloud"
inherit mbl-var-placeholders