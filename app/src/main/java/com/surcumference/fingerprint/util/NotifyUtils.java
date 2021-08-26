package com.surcumference.fingerprint.util;

import android.content.Context;
import android.content.pm.PackageInfo;
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
}
