package com.surcumference.fingerprint.view;

import android.app.AlertDialog;

import androidx.annotation.Nullable;

public class DialogUtils {

    public static void dismiss(@Nullable AlertDialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (IllegalArgumentException e) {
        }
    }
}
