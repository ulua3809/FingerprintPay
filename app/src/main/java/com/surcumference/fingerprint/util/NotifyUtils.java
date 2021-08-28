package com.surcumference.fingerprint.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.view.Gravity;
import android.widget.Toast;

import com.surcumference.fingerprint.util.log.L;

public class NotifyUtils {

    public static void notifyVersionUnSupport(final Context context, String packageName) {
        Task.onMain(()->{
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
                int versionCode = packageInfo.versionCode;
                String versionName = packageInfo.versionName;
                Toast.makeText(context, "当前版本:" + versionName + "." + versionCode + "不支持", Toast.LENGTH_LONG).show();
                L.d("当前版本:" + versionName + "." + versionCode + "不支持");
            } catch (Exception e) {
                L.e(e);
            }
        });
    }

    public static void notifyFingerprint(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
