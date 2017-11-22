SUMMARY = "i.MX7 Code Signing Tool - CSF"
DESCRIPTION = "Download and install NXP Code Signing Tool CSF file dependencies"
SECTION = "base"
LICENSE="Propriatery"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"

SRC_URI = "git://git@github.com/ARMmbed/mbl-tools.git;protocol=ssh;nobranch=1;destsuffix=${S}/git1;name=warp7-csf"
SRCREV_warp7-csf = "d30c7f205b52c21852ca60388c7bda5b0f2edff7"

LONGPATH = "git1/warp7-tools/imx7-code-signing/warp7-csf"

do_install() {
	install -d ${D}${sysconfdir}/cst
	install -d ${D}${sysconfdir}/cst/warp7/csf
	install -m 0755 ${LONGPATH}/* ${D}${sysconfdir}/cst/warp7/csf
}

BBCLASSEXTEND = "native nativesdk"