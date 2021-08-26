ui_print "- Extracting extra libraries"
extract "$ZIPFILE" "system/framework/lib$RIRU_MODULE_LIB_NAME.dex" "$MODPATH"
set_perm_recursive "$MODPATH" 0 0 0755 0644
