package com.surcumference.fingerprint.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.util.log.L;

public class NotifyUtils {

    public static void notifyVersionUnSupport(final Context context, String packageName) {
        Task.onMain(()->{
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
                int versionCode = packageInfo.versionCode;
                String versionName = packageInfo.versionName;
                Toaster.showLong("当前版本:" + versionName + "." + versionCode + "不支持");
                L.d("当前版本:" + versionName + "." + versionCode + "不支持");
            } catch (Exception e) {
                L.e(e);
            }
        });
    }

    public static void notifyFingerprint(Context context, String message) {
        if (Constant.PACKAGE_NAME_WECHAT.equals(context.getPackageName())) {
            // 支付界面无法弹出Toast
            View toastView = Toaster.getStyle().createView(context);
            TextView textView = (TextView)toastView.findViewById(android.R.id.message);
            textView.setText(message);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(toastView).create();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(0f);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();
            Task.onMain(2500, dialog::dismiss);
            return;
        }
        Toaster.showLong(message);
    }
}
