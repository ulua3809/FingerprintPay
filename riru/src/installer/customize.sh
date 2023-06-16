ui_print "Fingerprint Pay Batch Installer"
ls "$MODPATH"
ui_print "$ZIPFILE"
INSTALLER_MODPATH="$MODPATH"
for ZIPFILE in $MODPATH/*-release.zip; do
    ui_print "$ZIPFILE"
    install_module
done
rm -rf "$INSTALLER_MODPATH"