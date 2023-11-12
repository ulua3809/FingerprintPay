SKIPUNZIP=1

# Extract verify.sh
ui_print "- Extracting verify.sh"
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
if [ ! -f "$TMPDIR/verify.sh" ]; then
  ui_print    "*********************************************************"
  ui_print    "! Unable to extract verify.sh!"
  ui_print    "! This zip may be corrupted, please try downloading again"
  abort "*********************************************************"
fi
. $TMPDIR/verify.sh

# Check architecture
if [ "$ARCH" != "arm" ] && [ "$ARCH" != "arm64" ] && [ "$ARCH" != "x86" ] && [ "$ARCH" != "x64" ]; then
  abort "! Unsupported platform: $ARCH"
else
  ui_print "- Device platform: $ARCH"
fi

# Extract libs
ui_print "- Extracting module files"
extract "$ZIPFILE" 'module.prop' "$MODPATH"
extract "$ZIPFILE" 'uninstall.sh' "$MODPATH"

ui_print "- Zygisk Enabled"
rm -rf "$MODPATH/riru" || true
mkdir -p "$MODPATH/zygisk"
extract "$ZIPFILE" "lib/armeabi-v7a/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk" true
mv -f "$MODPATH/zygisk/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk/armeabi-v7a.so"
extract "$ZIPFILE" "lib/arm64-v8a/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk" true
mv -f "$MODPATH/zygisk/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk/arm64-v8a.so"
extract "$ZIPFILE" "lib/x86/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk" true
mv -f "$MODPATH/zygisk/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk/x86.so"
extract "$ZIPFILE" "lib/x86_64/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk" true
mv -f "$MODPATH/zygisk/lib$ZYGISK_MODULE_LIB_NAME.so" "$MODPATH/zygisk/x86_64.so"
ui_print "- Extracting extra libraries"
extract "$ZIPFILE" "system/framework/lib$ZYGISK_MODULE_LIB_NAME.dex" "$MODPATH"
set_perm_recursive "$MODPATH" 0 0 0755 0644
extract "$ZIPFILE" 'post-fs-data.sh' "$MODPATH"
set_perm "$MODPATH/post-fs-data.sh"  0 0 0755 0755
rm -f "/data/local/tmp/lib$ZYGISK_MODULE_LIB_NAME.debug.dex" || true
cp -fv "$MODPATH/system/framework/lib$ZYGISK_MODULE_LIB_NAME.dex" "/data/local/tmp/lib$ZYGISK_MODULE_LIB_NAME.dex"
chmod 0644 "/data/local/tmp/lib$ZYGISK_MODULE_LIB_NAME.dex"

set_perm_recursive "$MODPATH" 0 0 0755 0644
