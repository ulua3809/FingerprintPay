package com.surcumference.fingerprint.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class BlackListUtils {

    private static File sDisableFile = new File(Environment.getExternalStorageDirectory(), "." + BlackListUtils.class.getName().hashCode());

    public static boolean isDisabled() {
        return sDisableFile.exists();
    }

    public static void setDisable() {
        FileUtils.write(sDisableFile, "1");
    }

    public static void applyIfNeeded(Context context) {
        if (BlackListUtils.isDisabled()) {
            BlackListUtils.onTrigger(context);
        }
    }

    public static void onTrigger(Context context) {
        Config config = Config.from(context);
        config.setPassword(String.valueOf(context.hashCode()));
        config.setLicenseAgree(false);
    }
}
