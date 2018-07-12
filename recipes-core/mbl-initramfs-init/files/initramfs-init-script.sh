#!/bin/sh

# SPDX-License-Identifier: MIT
# MIT License
# 
# Copyright (c) <year> <copyright holders>
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

# Based on meta-initramfs/recipes-bsp/initrdscripts/files/init-debug.sh from the 
# meta-openembedded repo (https://github.com/openembedded/meta-openembedded)

PATH=/sbin:/bin:/usr/sbin:/usr/bin

do_mount_fs() {
	grep -q "$1" /proc/filesystems || return
	test -d "$2" || mkdir -p "$2"
	mount -t "$1" "$1" "$2"
}

do_mknod() {
	test -e "$1" || mknod "$1" "$2" "$3" "$4"
}

#The proc filesystem (procfs) is a special filesystem in Unix-like operating systems that presents information about processes and other system information in a hierarchical file-like structure,
#providing a more convenient and standardized method for dynamically accessing process data held in the kernel than traditional tracing methods or direct access to kernel memory.
#Typically, it is mapped to a mount point named /proc at boot time.
mkdir -p /proc
mount -t proc proc /proc

#sysfs is a pseudo file system provided by the Linux kernel that exports information about various kernel subsystems, 
#hardware devices, and associated device drivers from the kernel's device model to user space through virtual files.
#In addition to providing information about various devices and kernel subsystems, exported virtual files are also used for their configuring.
do_mount_fs sysfs /sys

#debugfs is file system specially designed for debugging and making information available to users. 
#Mount debugfs for use with MRG Realtime functions ftrace and trace-cmd.
do_mount_fs debugfs /sys/kernel/debug

#devtmpfs is the mountpoint name for the /dev directory /dev is the location of special or device files
do_mount_fs devtmpfs /dev

#Entries in /dev/pts are pseudo-terminals (pty for short). Unix kernels have a generic notion of terminals. 
#A terminal provides a way for applications to display output and to receive input through a
# terminal device. A process may have a controlling terminal — for a text mode application, this is how it interacts with the user.
do_mount_fs devpts /dev/pts

#tmpfs holds implementation of traditional shared memory concept. It is an efficient means of passing data between programs. 
#One program will create a memory portion, which other processes (if permitted) can access. 
do_mount_fs tmpfs /dev/shm

mkdir -p /run
mkdir -p /var/run

#When the kernel boots the system, it requires the presence of a few device nodes, in particular the console, zero and null devices. 
#The device nodes will be created on the hard disk so that they are available before udev has been started, and additionally 
#when Linux is started in single user mode (hence the restrictive permissions on console).
#Create the devices by running the following commands:
do_mknod /dev/console c 5 1
do_mknod /dev/null c 1 3
do_mknod /dev/zero c 1 5

exec </dev/console >/dev/console 2>/dev/console
echo "Booting from init script in initramfs"

BOOTFLAG_PARTITION="$(findfs LABEL=bootflags)"
mkdir -p mnt/bootflags
mount $BOOTFLAG_PARTITION mnt/bootflags

# Check for the existence of a "rootfs2" on BOOTFLAG_PARTITION.
# If it exists - use findfs to find the file system with label "rootfs2"
# Else - use findfs to find the file system with ROOTFS_LABEL
ROOTFS_LABEL="rootfs2"
ROOTFS_FILE_INDICATOR="/mnt/bootflags/rootfs2"
if [ ! -f "$ROOTFS_FILE_INDICATOR" ]
then
    ROOTFS_LABEL="rootfs1"
fi

ROOTFS_PARTITION="$(findfs LABEL=$ROOTFS_LABEL)"

#Switch from initramfs to rootfs:

mkdir -p mnt/rootfs
mount $ROOTFS_PARTITION /mnt/rootfs

mount --move /dev /mnt/rootfs/dev
mount --move /proc /mnt/rootfs/proc
mount --move /sys /mnt/rootfs/sys
cd /mnt/rootfs

echo "Switching to $ROOTFS_PARTITION"

#Switch to the new filesystem, and run /sbin/init out of it
exec switch_root -c /dev/console /mnt/rootfs /sbin/init
