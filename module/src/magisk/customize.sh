ui_print "- Riru Enabled"
rm -rf "$MODPATH/zygisk" || true
ui_print "- Extracting extra libraries"
extract "$ZIPFILE" "system/framework/lib$RIRU_MODULE_LIB_NAME.dex" "$MODPATH"
set_perm_recursive "$MODPATH" 0 0 0755 0644
extract "$ZIPFILE" 'post-fs-data.sh' "$MODPATH"
set_perm "$MODPATH/post-fs-data.sh"  0 0 0755 0755
rm -f "/data/local/tmp/lib$RIRU_MODULE_LIB_NAME.debug.dex" || true
cp -fv "$MODPATH/system/framework/lib$RIRU_MODULE_LIB_NAME.dex" "/data/local/tmp/lib$RIRU_MODULE_LIB_NAME.dex"
chmod 0644 "/data/local/tmp/lib$RIRU_MODULE_LIB_NAME.dex"
