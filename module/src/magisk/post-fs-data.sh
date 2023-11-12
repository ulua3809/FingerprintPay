#!/system/bin/sh
# Please don't hardcode /magisk/modname/... ; instead, please use $MODDIR/...
# This will make your scripts compatible even if Magisk change its mount point in the future
MODDIR=${0%/*}
cp -fv $MODDIR/system/framework/lib*.dex "/data/local/tmp/"
chmod 0644 /data/local/tmp/lib*.dex
# This script will be executed in post-fs-data mode
# More info in the main Magisk thread